package lambda.runners.scala.server

import cats.kernel.Monoid

case class ClassPath (cp: String)

object ClassPath {
  implicit val monoid: Monoid[ClassPath] = new Monoid[ClassPath] {
    def combine(x: ClassPath, y: ClassPath): ClassPath = ClassPath(
      List(x.cp, y.cp).mkString(":").split(":").toSet.filter(_.nonEmpty).mkString(":")
    )

    def empty: ClassPath = ClassPath(":")
  }
}
