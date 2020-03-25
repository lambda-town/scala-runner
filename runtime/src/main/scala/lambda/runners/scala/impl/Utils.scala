package lambda.runners.scala.impl

import java.io.File
import java.time.Instant

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.StrictLogging
import lambda.runners.scala.ScalaRunnerConfig
import org.apache.commons.io.FileUtils

import scala.util.Random

object Utils extends StrictLogging {

  case class ContainerFile(hostFile: File, containerFile: File)

  def getTmpOutputFolder(implicit config: ScalaRunnerConfig): Resource[IO, ContainerFile] = {
    val acquire = IO {
      val folderName = randomId()
      val newFolderInContainer = new File(config.tmpFilesRootPath.containerPath.toFile, folderName)
      val newFolderOnHost = new File(config.tmpFilesRootPath.hostPath.toFile, folderName)
      logger.debug("Creating a new temp folder {}", newFolderInContainer.getAbsolutePath)
      FileUtils.forceMkdir(newFolderInContainer)
      ContainerFile(newFolderOnHost, newFolderInContainer)
    }
    def release(folder: ContainerFile): IO[Unit] = IO {
      logger.debug("Deleting folder {}", folder.containerFile.getAbsolutePath)
      FileUtils.forceDelete(folder.containerFile)
    }
    Resource.make(acquire)(release)
  }

  def randomId() = s"${Instant.now().getNano}-${Random.alphanumeric.take(4).mkString}"
}
