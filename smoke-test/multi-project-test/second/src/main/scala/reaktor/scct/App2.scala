package reaktor.scct

object App2 {
  
  def foo(x : Array[String]) = x.foldLeft("")((a,b) => a + b)
  
  def main(args : Array[String]) = {
    println( "Hello World!" )
    println("concat arguments = " + foo(args))
    0
  }

}
