package lambda.runners.scala.messages

case class Dependency(
  organization: String,
  packageName: String,
  version: String,
  scalaVersion: String = "2.12"
)
