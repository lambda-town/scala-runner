package lambda.runners.scala.impl

import java.io.File
import java.time.Instant

import cats.effect.{IO, Resource}
import org.apache.commons.io.FileUtils

import scala.util.Random

object Utils {

  def getTmpOutputFolder: Resource[IO, File] = {
    val acquire = IO {
      val tmpDir = FileUtils.getTempDirectory()
      val newFilePath = new File(tmpDir, randomId())
      FileUtils.forceMkdirParent(newFilePath)
      newFilePath
    }
    def release(file: File): IO[Unit] = IO(FileUtils.forceDelete(file))
    Resource.make(acquire)(release)
  }

  def randomId() = s"${Instant.now().getNano}-${Random.alphanumeric.take(4).mkString}"
}
