import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"
  val nuProcess = "com.zaxxer" % "nuprocess" % "1.2.5"
  val fs2 = "co.fs2" %% "fs2-core" % "2.1.0"
  val pprint = "com.lihaoyi" %% "pprint" % "0.5.6"
  val commonsIO = "commons-io" % "commons-io" % "2.5"
  val programExecutor = "lambda" % "program-executor_2.12" % "0.3.0"

  object Coursier {
    lazy val core = "io.get-coursier" %% "coursier" % "2.0.0-RC2-6"
    lazy val interop = "io.get-coursier" %% "coursier-cats-interop" % "2.0.0-RC2-6"
    lazy val all = Seq(core, interop)
  }

  object Log {
    lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    lazy val all = Seq(logback, scalaLogging)
  }

  object Scala {
    lazy val version = "2.12.9"
    lazy val scalac = "org.scala-lang" % "scala-compiler" % version
    lazy val library = "org.scala-lang" % "scala-library" % version
    lazy val all = Seq(scalac, library)
  }
}
