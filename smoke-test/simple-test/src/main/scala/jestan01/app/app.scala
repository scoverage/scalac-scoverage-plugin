package jestan01.app



package object argh {
  case class Person()
}

class Foo {
  import jestan01.app.argh._
  object myRequestVar extends RequestVar[Person](Person())
  abstract class RequestVar[T](dflt: => T) {}
}

