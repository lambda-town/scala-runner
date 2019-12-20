package lambda.runners.scala.impl

import java.io.File
import java.time.Instant

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.StrictLogging
import lambda.runners.scala.ScalaRunnerConfig
import org.apache.commons.io.FileUtils

import scala.util.Random

object Utils extends StrictLogging {

  def getTmpOutputFolder(implicit config: ScalaRunnerConfig): Resource[IO, File] = {
    val acquire = IO {
      val newFilePath = new File(config.tmpFilesRootPath.toFile, randomId())
      logger.debug("Creating a new temp folder {}", newFilePath.getAbsolutePath)
      FileUtils.forceMkdir(newFilePath)
      newFilePath
    }
    def release(file: File): IO[Unit] = IO {
      logger.debug("Deleting folder {}", file.getAbsolutePath)
      FileUtils.forceDelete(file)
    }
    Resource.make(acquire)(release)
  }

  def randomId() = s"${Instant.now().getNano}-${Random.alphanumeric.take(4).mkString}"
}
