package scoverage.macrosupport

import scala.language.experimental.macros

object Tester {

  def test: Unit = macro TesterMacro.test

}
