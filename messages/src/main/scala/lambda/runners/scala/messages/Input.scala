package lambda.runners.scala.messages

sealed trait Input

object Input {
  case class FileInput(name: String, content: String) extends Input
  case class DependenciesInput(deps: List[Dependency]) extends Input
}
