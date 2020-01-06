package lambda.runners.scala.impl

import java.io.File

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import coursier.cache.FileCache
import coursier.core.{ModuleName, Organization}
import coursier.interop.cats._
import coursier.{Fetch, Module}
import fs2._
import lambda.programexecutor.ProgramEvent.{Exit, StdErr, StdOut}
import lambda.programexecutor._
import lambda.runners.scala.impl.Executor._
import lambda.runners.scala.{BuildInfo, Dependency, ScalaRunnerConfig}
import org.apache.commons.io.FileUtils

import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc._
import scala.tools.nsc.reporters.StoreReporter

object Compiler extends StrictLogging {

  def runCodeFiles(
      files: List[File],
      dependencies: List[Dependency],
  )(implicit config: ScalaRunnerConfig) =
    compileRunAndClean(files.map(fileToSourceFile), dependencies)

  def runCodeString(
      code: String,
      baseFiles: List[File],
      dependencies: List[Dependency],
  )(implicit config: ScalaRunnerConfig) =
    compileRunAndClean(baseFiles.map(fileToSourceFile) :+ stringToSourceFile(code), dependencies)

  private def compileSources(
      sources: List[BatchSourceFile],
      dependencies: List[Dependency],
      destFolder: File,
  ): Stream[IO, ProgramEvent] = {

    val reporter = new StoreReporter

    def formatInfo(info: reporter.Info) = {
      val extractPosRegex(pos) = info.pos.toString
      val path = s"sourceFile.scala,$pos"
      s"${info.severity} in $path : ${info.msg}"
    }

    val result =
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
            .collect({
              case i if i.severity == reporter.ERROR   => StdErr(formatInfo(i))
              case i if i.severity == reporter.WARNING => StdOut(formatInfo(i))
            })

        }

    Stream.evalSeq(result)
  }

  private def run(
      compiledClassesFolder: File,
  ) = {
    val cmd = List(
      "docker",
      "run",
      "-v",
      s"${compiledClassesFolder.getAbsolutePath}:/app/classpath",
      "--cpus",
      "1",
      BuildInfo.docker_imageNames.head,
    )
    Stream.eval(IO(logger.debug("Running Scala process"))) >> runProcess(cmd)
  }

  private def compileRunAndClean(
      sources: List[BatchSourceFile],
      dependencies: List[Dependency],
  )(implicit config: ScalaRunnerConfig): Stream[IO, ProgramEvent] = {
    Stream
      .resource(Utils.getTmpOutputFolder)
      .flatMap(outputFolder => {

        Stream
          .eval(Ref[IO].of(false))
          .flatMap(hasErrorsRef => {
            compileSources(sources, dependencies, outputFolder).evalTap({
              case _: StdErr => hasErrorsRef.set(true)
              case _         => IO.unit
            }) ++ Stream
              .eval(hasErrorsRef.get)
              .flatMap({
                case false => run(outputFolder)
                case _     => Stream[IO, ProgramEvent](Exit(1))
              })
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
