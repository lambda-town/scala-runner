import Dependencies._
import sbtghpackages.TokenSource.Environment

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "lambda"
ThisBuild / organizationName := "Lambdacademy"

ThisBuild / githubOwner := "lambdacademy-dev"
ThisBuild / githubTokenSource := Some(Environment("GITHUB_TOKEN"))
ThisBuild / githubUser := sys.env.getOrElse("GITHUB_USER", "REPLACE_ME")

lazy val root = (project in file("."))
  .settings(
    name := "scala-runner",
    githubRepository := "scala-runner",
    libraryDependencies ++= Seq(
      catsEffect,
      scalaTest % Test,
      nuProcess,
      fs2,
      commonsIO,
    ),
    dockerfile in docker := dockerFile
  ).enablePlugins(DockerPlugin)

lazy val scalaUtils = (project in file("utils"))
  .settings(
    name := "scala-utils",
    libraryDependencies += pprint
  )

lazy val dockerFile = new Dockerfile {
  from("tindzk/seed:0.1.5")
  workDir("/app")
  copy(file("docker"), ".")
  copy(file("utils/src/main/scala"), "./scala-utils")
  run("chmod", "+x", "./run.sh")
  run("chmod", "+x", "./startBloop.sh")
  entryPoint("./run.sh")
}