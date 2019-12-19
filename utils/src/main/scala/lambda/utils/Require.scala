package lambda.utils

import Emojis._

trait Require {

  def require(
      assertion: Boolean,
      message: String
  ): Unit =
    if (!assertion) println(message)

  def require[A](assertion: A => Boolean, message: String => String)(received: A): Unit =
    if (!assertion(received)) println(s"- $crossMark " + message(pprint.apply(received).plainText))

  def require[A](assertion: A => Boolean, successMessage: String, failureMessage: String => String)(
      received: A
  ): Unit = {
    val succeeded = assertion(received)
    if (succeeded) println(s"- $heavyCheckMark $successMessage")
    else require[A]((_: A) => false, failureMessage)(received)
  }
}

object Require extends Require
