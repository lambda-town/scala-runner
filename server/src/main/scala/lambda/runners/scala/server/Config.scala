package lambda.runners.scala.server

import pureconfig.generic.auto._

case class Config(host: String,
                  port: Int,
                  tmpFolder: String,
                  utilsClassPath: String,
                  maxNumberRunningProcesses: Long)

object Config {
  def load(): Config = pureconfig.loadConfigOrThrow[Config]
}
