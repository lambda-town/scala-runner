package lambda.utils

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers


class RequireSpec extends AnyFunSpec with Matchers {
  describe("Requirements Utils") {
    it("Should not throw when the assertion is met") {
      Require.require(true, "")
      Require.require[Int]((_: Int) => true, (_: String) => "msg")(42)
      succeed
    }

    it("Should print a success message when the assertion is met") {
      val successMessage = "Well done!"
      val (_, stdOut) = StdOut.captureStdOut {
        Require.require[Unit]((_: Unit) => true, successMessage, (_: String) => "")(())
      }
      stdOut should include(successMessage)
    }

    it("Should print the message when this simple assertion is not met") {
      val message = "You must have done something wrong"
      val (_, stdOut) = StdOut.captureStdOut({
        Require.require(false, message)
      })
      stdOut should include(message)
    }

    it("Should print a pretty version of the value when this assertion is not met") {
      type ValueType = List[Map[String, Option[Int]]]
      val value = List(
        Map("titi" -> Some(40), "toto" -> Some(42), "tata" -> Some(46)),
        Map.empty[String, Option[Int]],
        Map("lorem" -> None, "ipsum" -> Some(666))
      )
      val expectedMessage = s"Failed: ${pprint.apply(value).plainText}"

      val (_, stdOut: String) = StdOut.captureStdOut({
        Require.require[ValueType]((_: ValueType) => false, (pprinted: String) => s"Failed: $pprinted")(value)
      })

      stdOut should include(expectedMessage)
    }
  }
}
