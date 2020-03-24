package lambda.runners.scala

import java.net.InetSocketAddress

import cats.effect._
import fs2.io.tcp.SocketGroup

package object client {

  def build(host: String, port: Int)(implicit cs: ContextShift[IO], timer: Timer[IO]) =
    Blocker[IO].flatMap(
      blocker =>
        SocketGroup[IO](blocker).map(implicit sg => {
          implicit val addr = new InetSocketAddress(host, port)
          new ScalaRunnerClient()
        })
    )
}
