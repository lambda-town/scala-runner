package lambda.runners.scala.server

import java.io.File
import java.nio.file.Paths

import cats.effect.{IO, Resource}
import fs2._
import lambda.programexecutor.ProgramEvent
import lambda.programexecutor._
import org.apache.commons.io.FileUtils


object Runner extends {

  def run(
    cp: ClassPath,
    securityPolicyFile: File,
    mainClass: String = "Main",
  )(implicit config: Config): Stream[IO, ProgramEvent] = {
    val args = config.javaCommand.split(" ").toList ++ List(
      s"-Djava.security.policy==${securityPolicyFile.getAbsolutePath}",
      "-Xmx64m",
      "-Xshare:on",
      "-client",
      "-cp",
      cp.cp,
      mainClass
    )
    runProcess(args)
  }

  def securityPolicyFile(implicit config: Config): Resource[IO, File] =
    Utils.getTmpOutputFolder.evalMap(folder => IO {
      val destFile = Paths.get(folder.getAbsolutePath, "security.policy").toFile
      val resource = getClass.getClassLoader.getResourceAsStream("jvm-security.policy")
      FileUtils.copyInputStreamToFile(resource, destFile)
      destFile
    })
}
