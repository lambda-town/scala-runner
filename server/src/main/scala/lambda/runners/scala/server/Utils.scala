package lambda.runners.scala.server

import java.io.File
import java.time.Instant

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.FileUtils

import scala.util.Random

object Utils extends StrictLogging {

  def getTmpOutputFolder(implicit config: Config): Resource[IO, File] = {
    val acquire = IO {
      val folderName = randomId()
      val newFolder = new File(config.tmpFolder, folderName)
      logger.debug("Creating a new temp folder {}", newFolder.getAbsolutePath)
      FileUtils.forceMkdir(newFolder)
      newFolder
    }
    def release(folder: File): IO[Unit] = IO {
      logger.debug("Deleting folder {}", folder)
      FileUtils.forceDelete(folder)
    }
    Resource.make(acquire)(release)
  }

  def randomId() = s"${Instant.now().getNano}-${Random.alphanumeric.take(4).mkString}"
}
