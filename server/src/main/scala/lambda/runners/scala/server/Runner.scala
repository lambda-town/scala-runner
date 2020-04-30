package lambda.runners.scala.server

import cats.effect.IO
import fs2._
import lambda.programexecutor.ProgramEvent
import lambda.programexecutor._

object Runner extends {

  def run(cp: ClassPath, mainClass: String = "Main"): Stream[IO, ProgramEvent] = {
    val args = List(
      "java",
      "-Xshare:on",
      "-client",
      "-cp",
      cp.cp,
      mainClass
    )
    runProcess(args)
  }


}
