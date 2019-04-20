package scoverage.macrosupport

import scala.reflect.macros.blackbox.Context

private object TesterMacro {

  type TesterContext = Context { type PrefixType = Tester.type }

  def test(c: TesterContext) = {
    import c.universe._
    q"""println("macro test")"""
  }

}
