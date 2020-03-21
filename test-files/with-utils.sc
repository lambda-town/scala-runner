import lambda.utils.captureStdOut

import scala.util.Random

object Main extends App {
  val msg = Random.alphanumeric.take(10).mkString
  val (_, output) = captureStdOut({
    println(msg)
  })

  assert(output.trim == msg)
}