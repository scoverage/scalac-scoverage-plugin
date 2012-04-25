package reaktor.scct

object HelloWorld {
  def concat(x : Array[String]) = {
    x.foldLeft("")((a,b) => a + b)
  }

  def untestedMethod() = {
    val x = 12
    x * 2
  }
}
