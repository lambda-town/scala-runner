import Dependencies._
import sbtghpackages.TokenSource.Environment

ThisBuild / scalaVersion := "2.12.9"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "lambda"
ThisBuild / organizationName := "Lambdacademy"
ThisBuild / fork := true

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
    ) ++ Coursier.all ++ Log.all ++ Scala.all,
    dockerfile in docker := {
      new Dockerfile {
        from("hseeberger/scala-sbt:8u222_1.3.5_2.12.10")
        workDir("/app")
        copy(file("docker"), ".")
        copy(file("utils/src/main/scala"), "./scala-utils")
        add((assembly in scalaUtils).value, "./dependencies/utils.jar")
        run("chmod", "+x", "./run.sh")
        entryPoint("./run.sh")
      }
    },
    imageNames in docker := Seq(
      ImageName(s"${organization.value}/${name.value}:latest")
    ),
    buildInfoKeys := Seq[BuildInfoKey](version, imageNames in docker),
    buildInfoPackage := "lambda.runners.scala"
  )
  .dependsOn(scalaUtils)
  .enablePlugins(DockerPlugin, BuildInfoPlugin)

lazy val scalaUtils = (project in file("utils"))
  .settings(
    name := "scala-utils",
    libraryDependencies ++= Seq(
      pprint,
      scalaTest % Test,
    ),
    assemblyJarName in assembly := "utils.jar"
  )

