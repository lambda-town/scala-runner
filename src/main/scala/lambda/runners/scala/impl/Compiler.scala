package lambda.runners.scala.impl

import java.io.File

import cats.effect._
import cats.implicits._
import coursier.{Fetch, Module}
import coursier.interop.cats._
import coursier.cache.FileCache
import coursier.core.{ModuleName, Organization}
import lambda.runners.scala.{BuildInfo, Dependency, RunResult, ScalaRunnerConfig}
import fs2._
import org.apache.commons.io.FileUtils

import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.reporters.StoreReporter
import scala.tools.nsc._
import scala.sys.process._
import Executor._
import com.typesafe.scalalogging.StrictLogging

object Compiler extends StrictLogging {

  def runCodeFiles(
      files: List[File],
      dependencies: List[Dependency],
  )(implicit config: ScalaRunnerConfig): IO[RunResult[IO]] =
    compileRunAndClean(files.map(fileToSourceFile), dependencies)

  def runCodeString(
      code: String,
      baseFiles: List[File],
      dependencies: List[Dependency],
  )(implicit config: ScalaRunnerConfig): IO[RunResult[IO]] =
    compileRunAndClean(baseFiles.map(fileToSourceFile) :+ stringToSourceFile(code), dependencies)

  private def compileSources(
      sources: List[BatchSourceFile],
      dependencies: List[Dependency],
      destFolder: File,
  ): IO[List[String]] = {

    val reporter = new StoreReporter

    fetchDependencies(dependencies)
      .flatMap(deps =>
        IO {
          logger.debug("Compiling {} Scala sources", sources.length)

          val settings = new Settings()

          settings.embeddedDefaults(getClass.getClassLoader)
          settings.usejavacp.value = true

          deps.foreach(f => {
            settings.classpath.append(f.getAbsolutePath())
            settings.bootclasspath.append(f.getAbsolutePath())
          })
          settings.outdir.value = destFolder.getAbsolutePath

          val global = new Global(settings, reporter)
          val run = new global.Run

          run.compileSources(sources)

      }) *>
      IO {
        val infos = reporter.infos.toList
        infos
          .filter(i => i.severity == reporter.ERROR || i.severity == reporter.WARNING)
          .map(info => {
            val extractPosRegex(pos) = info.pos.toString
            val path = s"sourceFile.scala,$pos"
            s"${info.severity} in $path : ${info.msg}"
          })
      }
  }

  private def run(
      compiledClassesFolder: File,
  ) = {

    for {
      containerName <- IO { s"scala-${Utils.randomId()}" }
      killContainer = IO {
        s"docker kill $containerName".!!
        ()
      }
      cmd = List(
        "docker",
        "run",
        "-v",
        s"${compiledClassesFolder.getAbsolutePath}:/app/classpath",
        "--cpus",
        "1",
        BuildInfo.docker_imageNames.head,
      )
      result <- ProcessRunner.runProcess(cmd, killContainer)
    } yield result
  }

  private def compileRunAndClean(
      sources: List[BatchSourceFile],
      dependencies: List[Dependency],
  )(implicit config: ScalaRunnerConfig) = {
    Utils.getTmpOutputFolder.allocated.flatMap({
      case (outputFolder, deleteOutputFolder) =>
        compileSources(sources, dependencies, outputFolder).flatMap(errors => {
          if (errors.nonEmpty) {
            IO.pure(1)
              .start
              .map(
                fiber =>
                  RunResult(
                    Stream(),
                    Stream(errors: _*),
                    fiber
                ))
          } else {
            run(outputFolder).map(
              r =>
                r.copy(
                  stdOut = r.stdOut.onFinalize(deleteOutputFolder)
              ))
          }
        })
    })

  }

  private val extractPosRegex = ".*?,(.*)".r

  private def fetchDependencies(dependencies: List[Dependency]) =
    IO(
      logger.debug("Fetching {} dependencies with Coursier", dependencies.length)
    ) *>
      (if (dependencies.nonEmpty) {
         Fetch(cache)
           .addDependencies(dependencies.map(toCoursierDependency(_)): _*)
           .io
       } else IO(Nil))

  private val cache = FileCache[IO]()

  private def toCoursierDependency(dep: Dependency) = coursier.Dependency.of(
    Module(Organization(dep.organization), ModuleName(s"${dep.packageName}_${dep.scalaVersion}")),
    dep.version
  )

  private def fileToSourceFile(input: File) = {
    new BatchSourceFile(input.getName, FileUtils.readFileToByteArray(input).map(_.toChar))
  }

  private def stringToSourceFile(input: String) =
    new BatchSourceFile("user-input", input.toCharArray)

}