package lambda.runners.scala.impl

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant

import cats.effect.{IO, Resource}
import org.apache.commons.io.FileUtils

import scala.util.Random

object Utils {

  def writeTmpFile(content: String): Resource[IO, File] = {
    val acquire = IO {
      val tmpDir = FileUtils.getTempDirectory()
      val newFilePath = new File(tmpDir, randomFileName())
      FileUtils.writeStringToFile(
        newFilePath,
        content,
        StandardCharsets.UTF_8
      )
      newFilePath
    }

    def release(file: File): IO[Unit] = IO(FileUtils.forceDelete(file))

    Resource.make(acquire)(release)
  }

  private def randomFileName() = s"${Instant.now().toEpochMilli}-${Random.alphanumeric.take(8).mkString}"
}
