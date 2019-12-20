package lambda.runners.scala

import java.io.File
import java.nio.file.Paths

import cats.effect.IO
import org.scalatest.{AsyncFunSpec, Matchers}

class ScalaRunnerSpec extends AsyncFunSpec with Matchers {

  describe("The Scala Runner") {

    describe("When used with a simple string") {

      it("should compile and run the string") {

        val code = "object Main extends App { println(\"Test\") }"

        runResult(runCode(code))
          .map({
            case (exitCode, errors, stdOut) =>
              exitCode shouldBe 0
              errors shouldBe Nil
              stdOut shouldBe Nil
          })
          .unsafeToFuture()
      }
    }

    describe("When used with a file") {

      it("Should compile and run the file") {
        val file = testFile("compiles.sc")

        runResult(runFiles(List(file)))
          .map({
            case (exitCode, errors, stdOut) =>
              exitCode shouldBe 0
              errors shouldBe Nil
              stdOut shouldBe Nil
          })
          .unsafeToFuture()
      }
    }
  }

  private def runResult(rIO: IO[RunResult[IO]]): IO[(Int, List[String], List[String])] =
    for {
      r <- rIO
      exitCode <- r.exitCode.join
      errors <- r.stdErr.compile.toList
      stdOut <- r.stdOut.compile.toList
    } yield
      (
        exitCode,
        errors,
        stdOut
      )

  private def testFile(name: String): File =
    Paths.get(System.getProperty("user.dir"), "test-files", name).toFile

}
