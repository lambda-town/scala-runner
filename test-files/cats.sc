import cats.implicits._

object Main extends App {
  val Some(meow) = "Meow".some
  println(meow)
}