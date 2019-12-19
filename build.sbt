import Dependencies._
import sbtghpackages.TokenSource.GitConfig

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "lambda"
ThisBuild / organizationName := "Lambdacademy"
ThisBuild / githubOwner := "lambdacademy-dev"
ThisBuild / githubRepository := "scala-runner"
ThisBuild / githubTokenSource := Some(GitConfig("github.token"))

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
