package lambda.runners.scala

import java.io.File
import java.nio.file.Paths

import cats.effect._
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import lambda.programexecutor.ProgramEvent
import lambda.programexecutor.ProgramEvent.{Exit, StdErr, StdOut}
import lambda.runners.scala.messages.Dependency
import lambda.runners.scala.server.{Config, Server, Utils}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class ScalaRunnerSpec extends AsyncFunSpec with BeforeAndAfterAll with Matchers with StrictLogging {

  implicit private val cs = IO.contextShift(ExecutionContext.global)
  implicit private val timer = IO.timer(ExecutionContext.global)
  private var runningServer: Option[Fiber[IO, ExitCode]] = None
  private val config = Config.load()

  describe("When used with simple strings") {

    it("should compile and run the string") {

      val code = "object Main extends App { println(\"Test\") }"

      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.run(Map("test.sc" -> code), Nil))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors shouldBe Nil
            stdOut shouldBe List("Test")
            exitCode shouldBe 0
        })
        .unsafeToFuture()
    }

    it("Should fail at runtime") {

      val msg = s"ERROR : ${Utils.randomId()}"
      val code = s"""object Main extends App { throw new Error("$msg")}"""

      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.run(Map("test.sc" -> code), Nil))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors.mkString should include(msg)
            stdOut shouldBe Nil
            exitCode shouldBe 1
        })
        .unsafeToFuture()
    }

    it("Should fail at compile time") {
      val code = "object Main extends foo"
      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.run(Map("test.sc" -> code), Nil))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors.mkString should include("not found: type foo")
            stdOut shouldBe Nil
            exitCode shouldBe 1
        })
        .unsafeToFuture()
    }
  }

  describe("When used with files") {
    it("Should compile and run the file") {
      val file = testFile("compiles.sc")

      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.runFiles(List(file), Nil))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors shouldBe Nil
            stdOut shouldBe List("Hello World!")
            exitCode shouldBe 0
        })
        .unsafeToFuture()
    }

    it("Should fail at compile time") {
      val file = testFile("does-not-compile.sc")

      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.runFiles(List(file), Nil))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors.mkString should include("not found: type baz")
            stdOut shouldBe Nil
            exitCode shouldBe 1
        })
        .unsafeToFuture()
    }

    it("Should fail at runtime") {

      val file = testFile("fails.sc")
      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.runFiles(List(file), Nil))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors.mkString should include("Snap!")
            stdOut shouldBe List("Test")
            exitCode shouldBe 1
        })
        .unsafeToFuture()
    }

    it("Should download external dependencies") {

      val file = testFile("cats.sc")
      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.runFiles(List(file), List(Dependency("org.typelevel", "cats-core", "2.0.0"))))
      ).map({
          case (exitCode, stdOut, errors) =>
            errors shouldBe Nil
            stdOut shouldBe List("Meow")
            exitCode shouldBe 0
        })
        .unsafeToFuture()

    }

    it("Should be able to use methods from utils") {
      val file = testFile("with-utils.sc")
      result(
        Stream
          .resource(client.build(config.host, config.port))
          .flatMap(_.runFiles(List(file), Nil))
      ).map({
          case (exitCode, _, err) =>
            err shouldBe Nil
            exitCode shouldBe 0
        })
        .unsafeRunSync()
    }
  }

  override def beforeAll(): Unit = {
    runningServer = Some(Server.run(Nil).start.unsafeRunSync())
  }

  override def afterAll(): Unit = {
    logger.info("Stopping server")
    runningServer.get.cancel.unsafeRunSync()
  }

  private def result(s: Stream[IO, ProgramEvent]): IO[(Int, List[String], List[String])] =
    s.compile.toList.map(
      events =>
        (
          events
            .collectFirst({
              case Exit(code) => code
            })
            .get,
          events.collect({
            case StdOut(line) if line.nonEmpty => line.trim
          }),
          events.collect({
            case StdErr(line) if line.nonEmpty => line.trim
          })
        )
    )

  private def testFile(name: String): File =
    Paths.get(System.getProperty("user.dir"), "test-files", name).toFile
}
