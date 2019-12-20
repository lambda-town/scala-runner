package lambda.runners.scala.impl

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import cats.effect._
import com.zaxxer.nuprocess.{NuAbstractProcessHandler, NuProcessBuilder}
import fs2.concurrent.Queue
import lambda.runners.scala.RunResult
import Executor._
import fs2.Pipe

object ProcessRunner {

  def runProcess(commands: List[String], destroy: IO[Unit]): IO[RunResult[IO]] = {

    for {
      stdOutQueue <- Queue.unbounded[IO, Option[String]]
      stdErrQueue <- Queue.unbounded[IO, Option[String]]
      exitCodeIO <- IO
        .cancelable[Int] { cb =>
          val processHandler = new NuAbstractProcessHandler {

            override def onExit(statusCode: Int): Unit = {
              stdOutQueue.offer1(None).unsafeRunAsyncAndForget()
              stdErrQueue.offer1(None).unsafeRunAsyncAndForget()
              cb(Right(statusCode))
            }

            override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
              val bytes = new Array[Byte](buffer.remaining)
              buffer.get(bytes)
              stdOutQueue
                .offer1(Some(new String(bytes, StandardCharsets.UTF_8)))
                .unsafeRunAsyncAndForget()
            }

            override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
              val bytes = new Array[Byte](buffer.remaining)
              buffer.get(bytes)
              stdErrQueue
                .offer1(Some(new String(bytes, StandardCharsets.UTF_8)))
                .unsafeRunAsyncAndForget()
            }

          }
          val pb = new NuProcessBuilder(commands: _*)
          pb.setProcessListener(processHandler)
          val process = pb.start()

          destroy.flatMap(_ => IO(process.destroy(true)))
        }
        .start
      result = RunResult[IO](
        stdOutQueue.dequeue.unNoneTerminate.through(formatLines),
        stdErrQueue.dequeue.unNoneTerminate.through(formatLines),
        exitCodeIO
      )
    } yield result

  }

  private val formatLines: Pipe[IO, String, String] = stream => stream.map(_.trim).filter(_.nonEmpty)

}
