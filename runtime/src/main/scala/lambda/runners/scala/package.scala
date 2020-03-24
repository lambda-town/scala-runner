package lambda.runners

import cats.effect._
import lambda.programexecutor.ProgramEvent
import lambda.runners.scala.messages.Dependency

package object scala {

  /**
    * Evaluates scala code
    * @param files some scala code
    * @param dependencies a list of external dependencies to fetch
    * @return
    */
  def runCode(
      files: Map[String, String] = Map.empty,
      dependencies: List[Dependency] = Nil
  )(implicit config: ScalaRunnerConfig): fs2.Stream[IO, ProgramEvent] = impl.Compiler.run(files, dependencies)

}
