package reaktor.scct

object App3 {
  
  def foo(x : Array[String]) = App2.foo(x)
  
  def main(args : Array[String]) = {
    println("Internal project dependency....")
    println("... arguments = " + foo(args))
    0
  }

}
