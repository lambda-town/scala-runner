package lambda.runners.scala

import cats.effect.Fiber
import fs2._

case class RunResult[F[_]](
  stdOut: Stream[F, String],
  stdErr: Stream[F, String],
  exitCode: Fiber[F, Int]
)
