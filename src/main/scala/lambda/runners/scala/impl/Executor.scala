package lambda.runners.scala.impl

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext

object Executor {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
    Executors.newCachedThreadPool()
  )

  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

}
