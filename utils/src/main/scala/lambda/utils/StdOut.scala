package lambda.utils

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

trait StdOut {
  def captureStdOut[A](block: => A): (A, String) = {
    val stream = new ByteArrayOutputStream()
    val result = Console.withOut(stream) {
      Console.withErr(stream) {
        block
      }
    }
    (result, new String(stream.toByteArray(), StandardCharsets.UTF_8))
  }
}

object StdOut extends StdOut