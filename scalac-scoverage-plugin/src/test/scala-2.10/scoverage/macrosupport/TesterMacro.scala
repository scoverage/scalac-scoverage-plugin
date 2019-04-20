package scoverage.macrosupport

import scala.reflect.macros.Context

private object TesterMacro {

  type TesterContext = Context { type PrefixType = Tester.type }

  def test(c: TesterContext) =
    c.universe.reify(
      println("macro test")
    )

}
