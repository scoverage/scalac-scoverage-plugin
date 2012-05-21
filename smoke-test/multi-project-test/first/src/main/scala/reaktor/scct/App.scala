package reaktor.scct

object App {
  def foo(x : Array[String]) = x.foldLeft("")((a,b) => a + b)
  
  def bar(x : Array[String]) = {
    x.foldLeft("")((a,b) => a + b)
  }
  
  def main(args : Array[String]) {
    println( "Hello World!" )
    println("concat arguments = " + foo(args))
  }
}
