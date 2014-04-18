package scoverage

import org.scalatest.{BeforeAndAfterEachTestData, OneInstancePerTest, FunSuite}
import org.scalatest.mock.MockitoSugar

/** @author Stephen Samuel */
class PluginCoverageTest
  extends FunSuite
  with MockitoSugar
  with OneInstancePerTest
  with PluginSupport
  with BeforeAndAfterEachTestData {

  test("scoverage should instrument default arguments with methods") {
    compileCodeSnippet( """ object DefaultArgumentsObject {
                          |  val defaultName = "world"
                          |  def makeGreeting(name: String = defaultName): String = {
                          |    s"Hello, $name"
                          |  }
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    // we should have 2 statements - initialising the val and executing string sub in the def
    assert(2 === compiler.instrumentationComponent.coverage.statements.size)
  }

  test("scoverage should instrument private final vals") {
    compileCodeSnippet( """ object FinalVals {
                          |  private final val name = "sammy"
                          |  println(name)
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    // we should have 3 statements - initialising the val, entering the method, and executing the parameter
    assert(3 === compiler.instrumentationComponent.coverage.statements.size)
  }
}
