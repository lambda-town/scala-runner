package lambda.runners.scala.client

import java.io.File
import java.net.{ConnectException, InetSocketAddress}
import java.nio.charset.StandardCharsets

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import fs2.io.tcp.{Socket, SocketGroup}
import io.circe.fs2._
import lambda.runners.scala.messages.{Dependency, Input}
import lambda.runners.scala.messages.Serialization._
import io.circe.syntax._

import scala.concurrent.duration._
import fs2._
import lambda.programexecutor.ProgramEvent
import org.apache.commons.io.FileUtils

class ScalaRunnerClient(
    implicit socketGroup: SocketGroup,
    addr: InetSocketAddress,
    cs: ContextShift[IO],
    timer: Timer[IO]
) extends StrictLogging {

  def runFiles(files: List[File], dependencies: List[Dependency]) = {
    Stream(files: _*)
      .evalMap(f => IO((f.getName, FileUtils.readFileToString(f, StandardCharsets.UTF_8))))
      .chunkLimit(files.length)
      .map(_.toList.toMap)
      .flatMap(run(_, dependencies))
  }

  def run(files: Map[String, String], dependencies: List[Dependency]) =
    connect().flatMap { socket =>
      val input: List[Input] = files.map(Input.FileInput.tupled).toList :+ Input.DependenciesInput(dependencies)
      Stream(input: _*)
        .map(_.asJson.noSpaces)
        .interleave(Stream.constant("\n"))
        .through(text.utf8Encode)
        .through(socket.writes())
        .append(Stream.eval(socket.endOfOutput))
        .drain
        .append(socket.reads(16000))
        .through(byteStreamParser)
        .through(decoder[IO, ProgramEvent])
        .handleErrorWith(e => Stream.eval(IO(logger.error("Something went wrong on the client", e)) >> IO.raiseError(e)))
    }

  private def connect(attempt: Int = 1): Stream[IO, Socket[IO]] =
    Stream
      .resource(socketGroup.client[IO](addr))
      .evalTap(_ => IO(println(s"Connected to Scala Runner Server running at $addr")))
      .handleErrorWith {
        case e: ConnectException =>
          if (attempt < 5) connect(attempt + 1).delayBy(5.seconds * attempt.toLong)
          else Stream.raiseError[IO](e)
      }
}
