package lambda.runners.scala

import java.io.File
import java.nio.file.Paths

import fs2._
import cats.effect.IO
import lambda.programexecutor.ProgramEvent
import lambda.programexecutor.ProgramEvent._
import lambda.runners.scala.impl.Utils
import org.scalatest.{AsyncFunSpec, Matchers}

class ScalaRunnerSpec extends AsyncFunSpec with Matchers {

  describe("The Scala Runner") {

    describe("When used with a simple string") {

      it("should compile and run the string") {

        val code = "object Main extends App { println(\"Test\") }"
        result(runCode(code))
          .map({
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
        result(runCode(code))
          .map({
            case (exitCode, stdOut, errors) =>
              errors.mkString should include(msg)
              stdOut shouldBe Nil
              exitCode shouldBe 1
          })
          .unsafeToFuture()
      }

      it("Should fail at compile time") {
        val code = "object Main extends foo"
        result(runCode(code))
          .map({
            case (exitCode, stdOut, errors) =>
              errors.mkString should include("not found: type foo")
              stdOut shouldBe Nil
              exitCode shouldBe 1
          })
          .unsafeToFuture()
      }
    }

    describe("When used with a file") {

      it("Should compile and run the file") {
        val file = testFile("compiles.sc")

        result(runFiles(List(file)))
          .map({
            case (exitCode, stdOut, errors) =>
              errors shouldBe Nil
              stdOut shouldBe List("Hello World!")
              exitCode shouldBe 0
          })
          .unsafeToFuture()
      }

      it("Should fail at compile time") {
        val file = testFile("does-not-compile.sc")

        result(runFiles(List(file)))
          .map({
            case (exitCode, stdOut, errors) =>
              errors.mkString should include("not found: type baz")
              stdOut shouldBe Nil
              exitCode shouldBe 1
          })
          .unsafeToFuture()
      }

      it("Should fail at runtime") {

        val file = testFile("fails.sc")
        result(runFiles(List(file)))
          .map({
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
          runFiles(
            List(file),
            List(
              Dependency("org.typelevel", "cats-core", "2.0.0")
            )
          ))
          .map({
            case (exitCode, stdOut, errors) =>
              errors shouldBe Nil
              stdOut shouldBe List("Meow")
              exitCode shouldBe 0
          })
          .unsafeToFuture()

      }

      it("Should be able to use methods from utils") {
        val file = testFile("with-utils.sc")
        result(runFiles(List(file))).map({
          case (exitCode, _, err) =>
            err shouldBe Nil
            exitCode shouldBe 0
        }).unsafeRunSync()
      }

    }
  }

  implicit lazy val config: ScalaRunnerConfig = ScalaRunnerConfig.default()

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
      ))

  private def testFile(name: String): File =
    Paths.get(System.getProperty("user.dir"), "test-files", name).toFile

}
