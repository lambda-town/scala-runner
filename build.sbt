import Dependencies._
import sbtghpackages.TokenSource.Environment

ThisBuild / scalaVersion := "2.12.9"
ThisBuild / version := "0.2.2"
ThisBuild / organization := "lambda"
ThisBuild / organizationName := "Lambdacademy"
ThisBuild / fork := true

ThisBuild / githubOwner := "lambdacademy-dev"
ThisBuild / githubTokenSource := Some(Environment("GITHUB_TOKEN"))
ThisBuild / githubUser := sys.env.getOrElse("GITHUB_USER", "REPLACE_ME")
ThisBuild / githubRepository := "scala-runner"

ThisBuild / resolvers ++= Seq("program-executor").map(Resolver.githubPackagesRepo("lambdacademy-dev", _))

lazy val root = (project in file("."))
  .settings(
    name := "scala-runner",
    libraryDependencies ++= Seq(
      catsEffect,
      scalaTest % Test,
      nuProcess,
      fs2,
      commonsIO,
      programExecutor,
    ) ++ Coursier.all ++ Log.all ++ Scala.all,
    dockerfile in docker := {
      val v = (ThisBuild / scalaVersion).value
      new Dockerfile {
        from("azul/zulu-openjdk-alpine:8")
        workDir("/scala")
        run("apk" , "add", "--no-cache", "--virtual=.build-dependencies", "wget", "ca-certificates", "bash")
        run("wget", "--no-verbose", s"https://downloads.lightbend.com/scala/$v/scala-$v.tgz")
        run("tar", "xzf", s"scala-$v.tgz")
        run("rm", "-rf", ".build-dependencies")
        run("rm", s"scala-$v.tgz")
        workDir("/app")
        copy(file("docker"), ".")
        copy(file("utils/src/main/scala"), "./scala-utils")
        add((assembly in scalaUtils).value, "./dependencies/utils.jar")
        run("chmod", "+x", "./run.sh")
        entryPoint("./run.sh")
      }
    },
    imageNames in docker := Seq(version.value, "LATEST").map(version =>
      ImageName(s"docker.pkg.github.com/${githubOwner.value}/${githubRepository.value}/${name.value}:$version")
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

