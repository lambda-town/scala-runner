package lambda.runners.scala.messages

import io.circe._
import io.circe.generic.semiauto._
import lambda.programexecutor.ProgramEvent

object Serialization {

  implicit val dependencyCodec: Codec[Dependency] = deriveCodec
  implicit val processEventCodec: Codec[ProgramEvent] = deriveCodec
  implicit val inputCodec: Codec[Input] = deriveCodec
}
