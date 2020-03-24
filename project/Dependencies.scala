import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"
  val commonsIO = "commons-io" % "commons-io" % "2.5"
  val programExecutor = "lambda" % "program-executor_2.12" % "0.3.0"
  val pprint = "com.lihaoyi" %% "pprint" % "0.5.6"

  object PureConfig {
    private val version = "0.10.1"
    val core = "com.github.pureconfig" %% "pureconfig" % version
    val all: Seq[ModuleID] = Seq(core)
  }

  object Fs2 {
    private val version = "2.1.0"

    val core =  "co.fs2" %% "fs2-core" % version
    val io = "co.fs2" %% "fs2-io" % version

    val all = Seq(core, io)
  }

  object Circe {
    private val version = "0.13.0"

    val core = "io.circe" %% "circe-core" % version
    val generic = "io.circe" %% "circe-generic" % version
    val genericExtras = "io.circe" %% "circe-generic-extras" % version
    val fs2 = "io.circe" % "circe-fs2_2.12" % version

    val all: Seq[ModuleID] = Seq(core, generic, genericExtras, fs2)
  }

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
    lazy val version = "2.12.11"
    lazy val scalac = "org.scala-lang" % "scala-compiler" % version
    lazy val library = "org.scala-lang" % "scala-library" % version
    lazy val all = Seq(scalac, library)
  }
}
