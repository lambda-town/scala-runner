package lambda.runners

import java.io.File

import cats.effect._
import lambda.runners.scala.impl.Utils

package object scala {

  /**
    * Compiles and runs scala files
    * @param sourceFiles a list of files to compile
    * @param dependencies a list of external dependencies to fetch
    * @return
    */
  def runFiles(
      sourceFiles: List[File],
      dependencies: List[Dependency] = Nil,
  ): IO[RunResult[IO]] = ???

  /**
    * Evaluates a scala string, as if it was written in the main of an application
    * @param code some scala code
    * @param dependencies a list of external dependencies to fetch
    * @return
    */
  def runCode(
      code: String,
      dependencies: List[Dependency] = Nil,
  ): IO[RunResult[IO]] =
    Utils
      .writeTmpFile(s"object Main extends App {$code}")
      .use(file => {
        runFiles(List(file), dependencies)
      })
}
