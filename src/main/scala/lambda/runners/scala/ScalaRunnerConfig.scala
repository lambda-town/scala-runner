package lambda.runners.scala

import java.nio.file.{Path, Paths}

/**
* A configuration object for the Scala Runner
 * @param tmpFilesRootPath this path must be known of Docker on MacOS because it will be mounted inside a container.
 */
case class ScalaRunnerConfig (
    tmpFilesRootPath: Path
)

object ScalaRunnerConfig {
  def default(): ScalaRunnerConfig = {
    val tmpFolder = Paths.get(System.getProperty("user.dir"), "tmp")
    ScalaRunnerConfig(tmpFolder)
  }
}