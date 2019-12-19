package lambda.runners

import java.io.File

import cats.effect._
import fs2._

package object scala {

  def run[F[_]: Sync](
    sourceFiles: List[File],
    dependencies: List[Dependency] = Nil,
  ): RunResult[F] = ???
}
