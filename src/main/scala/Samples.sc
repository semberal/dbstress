
//val a: Either[String, Int] = Right(5)
val a: Either[String, Int] = Left("Error A")
val b: Either[String, Int] = Right(8)
//val c: Either[String, Int] = Right(5)
val c: Either[String, Int] = Left("Error C")
for {
  x <- a.right
  y <- b.right
  z <- c.right
} yield {
  x+y+z
}