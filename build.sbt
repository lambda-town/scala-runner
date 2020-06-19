import Dependencies._

ThisBuild / scalaVersion := "2.12.11"
ThisBuild / version := "0.5.0"
ThisBuild / organization := "lambda"
ThisBuild / organizationName := "Lambda Town"
ThisBuild / fork := true

ThisBuild / githubOwner := "lambda-town"
ThisBuild / resolvers += Resolver.githubPackages("lambda-town")
ThisBuild / githubRepository := "scala-runner"

lazy val root = (project in file("."))
  .settings(
    name := "scala-runner",
    libraryDependencies += scalaTest % Test,
    githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")
  )
  .dependsOn(server, client, scalaUtils)

lazy val server = (project in file("server"))
  .settings(
    name := "scala-runner-server",
    libraryDependencies ++= PureConfig.all ++ Circe.all ++ Fs2.all ++ Log.all ++ Coursier.all ++ Scala.all ++ Seq(
      programExecutor,
      commonsIO
    ),
    githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN"),
    dockerfile in docker := {
      val v = (ThisBuild / scalaVersion).value
      new Dockerfile {
        from("azul/zulu-openjdk-alpine:13")
        expose(2003)
        workDir("/app")
        // Create Class Data Archive
        run("java", "-Xshare:dump")
        add((assembly in scalaUtils).value, "./dependencies/utils.jar")
        add(assembly.value, "./server.jar")
        entryPoint(
          "java",
          "-Dconfig.resource=application-prod.conf",
          "-Xms92m",
          "-Xmx92m",
          "-XX:+CrashOnOutOfMemoryError",
          "-jar",
          "./server.jar"
        )
      }
    },
    imageNames in docker := Seq(version.value, "latest").map(version =>
      ImageName(s"rg.fr-par.scw.cloud/lambda/${name.value}:$version")
    ),
  ).dependsOn(messages).enablePlugins(DockerPlugin)

lazy val client = (project in file("client"))
  .settings(
    name := "scala-runner-client",
    libraryDependencies ++= PureConfig.all ++ Circe.all ++ Log.all ++ Fs2.all :+ commonsIO,
    githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")
  ).dependsOn(messages)

lazy val messages = (project in file("messages"))
  .settings(
    name := "scala-runner-messages",
    libraryDependencies ++= Circe.all :+ programExecutor,
    githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")
  )

lazy val scalaUtils = (project in file("utils"))
  .settings(
    name := "scala-utils",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      pprint
    ),
    assemblyJarName in assembly := "utils.jar",
    githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")
  )

