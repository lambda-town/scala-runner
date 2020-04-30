package lambda.runners.scala.server

import java.net.InetSocketAddress

import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Semaphore
import com.typesafe.scalalogging.StrictLogging
import lambda.runners.scala.messages.Serialization._
import io.circe.Json
import io.circe.fs2._
import io.circe.syntax._
import fs2._
import fs2.io.tcp.SocketGroup
import lambda.programexecutor.ProgramEvent
import lambda.runners.scala.messages.Input

import scala.util.Random

object Server extends IOApp with StrictLogging {

  implicit val serverConfig = Config.load()
  val compiler = new Compiler()

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { blocker =>
      for {
        _ <- IO(logger.info("Downloading common dependencies"))
        _ <- compiler.fetchCommonDependencies()
        _ <- IO(logger.info("Downloaded common dependencies", serverConfig.host, serverConfig.port))
        permits <- Semaphore[IO](serverConfig.maxNumberRunningProcesses)
        _ <- IO(logger.info("Scala Runner Server is running on {}:{}", serverConfig.host, serverConfig.port))
        _ <- SocketGroup[IO](blocker).use(server(_, permits))
      } yield ExitCode.Success
    }

  private def server(socketGroup: SocketGroup, permits: Semaphore[IO]) = {
    (socketGroup.server[IO](new InetSocketAddress(serverConfig.host, serverConfig.port)) map { socketResource =>
      Stream
        .resource(socketResource)
        .evalMap(socket => permits.acquire.as(socket))
        .flatMap(
          socket =>
            socket
              .reads(16000)
              .through(byteStreamParser)
              .through(run(Random.alphanumeric.take(15).mkString))
              .map(_.asJson.noSpaces)
              .interleave(Stream.constant("\n"))
              .through(text.utf8Encode)
              .through(socket.writes())
              .append(Stream.eval(socket.endOfOutput))
              .handleError(e => logger.error("Something went wrong on the server", e))
        )
        .evalTap(_ => permits.release)
    }).parJoinUnbounded.compile.drain
  }

  private def run(id: String): Pipe[IO, Json, ProgramEvent] =
    _.evalTap(json => IO(logger.trace("Received JSON {} (id: {})", json, id)))
      .through(decoder[IO, Input])
      .evalTap(input => IO(logger.debug("Received Input {} (id: {})", input, id)))
      .fold(List.empty[Input])({
        case (list, input) => list :+ input
      })
      .flatMap(input => {
        val files = input
          .collect({
            case Input.FileInput(name, content) => (name, content)
          })
          .toMap

        val deps = input
          .collect({
            case Input.DependenciesInput(deps) => deps
          })
          .flatten

        Stream.resource(compiler.compile(files, deps)).flatMap({
          case Left(errors) =>
            Stream(
            errors.map(ProgramEvent.StdErr):_*
          ) ++ Stream(ProgramEvent.Exit(1))
          case Right(cp) => Runner.run(cp)
        })
      })
      .evalTap(evt => IO(logger.trace("Emitting event {} (id: {})", evt, id)))
      .handleErrorWith(
        e =>
          Stream.eval(IO {
            logger.warn("Something went wrong while running the code", e)
            ProgramEvent.Exit(1)
          })
      )
}
