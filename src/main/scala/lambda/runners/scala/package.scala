package lambda.runners

import java.io.File

import cats.effect._
import lambda.programexecutor.ProgramEvent

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
  )(implicit config: ScalaRunnerConfig): fs2.Stream[IO, ProgramEvent] = impl.Compiler.runCodeFiles(sourceFiles, dependencies)

  /**
    * Evaluates a scala string, as if it was written in the main of an application
    * @param code some scala code
    * @param dependencies a list of external dependencies to fetch
    * @return
    */
  def runCode(
      code: String,
      baseFiles: List[File] = Nil,
      dependencies: List[Dependency] = Nil,
  )(implicit config: ScalaRunnerConfig): fs2.Stream[IO, ProgramEvent] = impl.Compiler.runCodeString(code, baseFiles, dependencies)
}
