package lambda.runners.scala

import java.nio.file.Path

import lambda.runners.scala.ScalaRunnerConfig.Volume
import pureconfig.generic.auto._

/**
* A configuration object for the Scala Runner
 */
case class ScalaRunnerConfig (
    tmpFilesRootPath: Volume
)

object ScalaRunnerConfig {

  case class Volume(hostPath: Path, containerPath: Path)

  def load(): ScalaRunnerConfig = pureconfig.loadConfigOrThrow[ScalaRunnerConfig]
}