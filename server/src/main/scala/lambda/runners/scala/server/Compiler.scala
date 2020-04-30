package lambda.runners.scala.server

import cats.effect._
import cats.implicits._
import coursier._
import coursier.cache.FileCache
import coursier.interop.cats._
import lambda.runners.scala.messages.Dependency

import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.reporters.StoreReporter
import scala.tools.nsc.{Global, Settings}

class Compiler(implicit config: Config, cs: ContextShift[IO]) {

  def compile(files: Map[String, String], deps: Seq[Dependency]): Resource[IO, Either[List[String], ClassPath]] =
    Utils.getTmpOutputFolder.evalMap(outputFolder => fetchDependencies(deps).flatMap(dependenciesCp => IO {

      val baseClassPath = dependenciesCp |+| ClassPath(config.utilsClassPath)

      val reporter = new StoreReporter
      val settings = new Settings()

      settings.classpath.append(baseClassPath.cp)
      // settings.bootclasspath.append(baseClassPath.cp)
      settings.usejavacp.value = true
      settings.outdir.value = outputFolder.getAbsolutePath

      val global = new Global(settings, reporter)
      val run = new global.Run

      val sources = files.map({
        case (name, content) => new BatchSourceFile(name, content)
      }).toList

      run.compileSources(sources)

      val errors = reporter.infos.toList
        .collect({
          case i if i.severity == reporter.ERROR   => formatInfo(reporter)(i)
        })

      val finalClassPath = baseClassPath |+| ClassPath(outputFolder.getAbsolutePath)

      Either.cond(errors.isEmpty, finalClassPath, errors)
    }))

  def fetchCommonDependencies(): IO[Unit] = {
    Fetch[IO](coursierCache).addDependencies(commonDependencies.map(toCoursierDependency):_*).io.as(())
  }

  private def fetchDependencies(deps: Seq[Dependency]): IO[ClassPath] = {
    Fetch[IO](coursierCache).addDependencies((deps ++ commonDependencies).map(toCoursierDependency):_*)
      .io
      .map(_.map(f => ClassPath(f.getAbsolutePath)).toList.combineAll)
  }

  private val commonDependencies = List(
    Dependency(
      "org.scala-lang",
      "scala-library",
      "2.12.11",
      scalaVersion = ""
    )
  )

  private lazy val coursierCache: FileCache[IO] = FileCache[IO]()

  private def toCoursierDependency(dep: Dependency) = {
    val moduleName = Option(dep.scalaVersion).filter(_.nonEmpty).fold(dep.packageName)(v => s"${dep.packageName}_$v")
    coursier.Dependency.of(
      Module(
        Organization(dep.organization),
        ModuleName(moduleName)
      ),
      dep.version
    )
  }

  def formatInfo(reporter: StoreReporter)(info: reporter.Info) = {
    val extractPosRegex(pos) = info.pos.toString
    val path = s"sourceFile.scala,$pos"
    s"${info.severity} in $path : ${info.msg}"
  }

  private lazy val extractPosRegex = ".*?,(.*)".r

}
