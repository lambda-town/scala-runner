import Dependencies._
import sbtghpackages.TokenSource.Environment

ThisBuild / scalaVersion := "2.12.11"
ThisBuild / version := "0.3.4"
ThisBuild / organization := "lambda"
ThisBuild / organizationName := "Lambdacademy"
ThisBuild / fork := true

ThisBuild / githubOwner := "lambdacademy-dev"
ThisBuild / githubTokenSource := Some(Environment("GITHUB_TOKEN"))
ThisBuild / githubUser := sys.env.getOrElse("GITHUB_USER", "REPLACE_ME")
ThisBuild / githubRepository := "scala-runner"

ThisBuild / resolvers ++= Seq("program-executor", "scala-runer").map(Resolver.githubPackagesRepo("lambdacademy-dev", _))

lazy val root = (project in file("."))
  .settings(
    name := "scala-runner",
    libraryDependencies += scalaTest % Test
  )
  .dependsOn(server, client)

lazy val server = (project in file("server"))
  .settings(
    name := "scala-runner-server",
    libraryDependencies ++= PureConfig.all ++ Circe.all ++ Fs2.all ++ Log.all,
    dockerfile in docker := {
      val v = (ThisBuild / scalaVersion).value
      new Dockerfile {
        from("azul/zulu-openjdk-alpine:13")
        run("apk" , "add", "docker-cli")
        expose(2003)
        workDir("/app")
        add(assembly.value, "./server.jar")
        entryPoint("java", "-jar", "./server.jar")
      }
    },
    imageNames in docker := Seq(version.value, "LATEST").map(version =>
      ImageName(s"docker.pkg.github.com/${githubOwner.value}/${githubRepository.value}/${name.value}:$version")
    ),
  ).dependsOn(runtime, messages).enablePlugins(DockerPlugin)

lazy val client = (project in file("client"))
  .settings(
    name := "scala-runner-client",
    libraryDependencies ++= PureConfig.all ++ Circe.all ++ Log.all ++ Fs2.all :+ commonsIO
  ).dependsOn(messages)

lazy val messages = (project in file("messages"))
  .settings(
    name := "scala-runner-messages",
    libraryDependencies ++= Circe.all :+ programExecutor
  )

lazy val runtime = (project in file("runtime"))
  .settings(
    name := "scala-runner-runtime",
    libraryDependencies ++= Seq(
      catsEffect,
      Fs2.core,
      commonsIO,
      programExecutor,
    ) ++ Coursier.all ++ Log.all ++ Scala.all ++ PureConfig.all,
    dockerfile in docker := {
      val v = (ThisBuild / scalaVersion).value
      new Dockerfile {
        from("azul/zulu-openjdk-alpine:13")
        workDir("/scala")
        run("apk" , "add", "--no-cache", "--virtual=.build-dependencies", "wget", "ca-certificates", "bash")
        run("wget", "--no-verbose", s"https://downloads.lightbend.com/scala/$v/scala-$v.tgz")
        run("tar", "xzf", s"scala-$v.tgz")
        run("rm", "-rf", ".build-dependencies")
        run("rm", s"scala-$v.tgz")
        workDir("/app")
        copy(file("docker"), ".")
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
  .dependsOn(scalaUtils, messages)
  .enablePlugins(DockerPlugin, BuildInfoPlugin)

lazy val scalaUtils = (project in file("utils"))
  .settings(
    name := "scala-utils",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      pprint
    ),
    assemblyJarName in assembly := "utils.jar"
  )

