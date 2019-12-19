import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "lambda"
ThisBuild / organizationName := "Lambdacademy"

lazy val root = (project in file("."))
  .settings(
    name := "scala-runner",
    libraryDependencies ++= Seq(
      catsEffect,
      scalaTest % Test,
      nuProcess,
      fs2
    )
  )

lazy val scalaUtils = (project in file("utils"))
  .settings(
    name := "scala-utils"
  )