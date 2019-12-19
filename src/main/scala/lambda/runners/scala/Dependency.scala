package lambda.runners.scala

case class Dependency(
  organization: String,
  packageName: String,
  version: String,
  scalaVersion: String = "2.12"
)
