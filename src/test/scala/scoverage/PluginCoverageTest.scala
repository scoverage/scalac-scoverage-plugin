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
    assertNMeasuredStatements(2)
  }

  test("scoverage should instrument private final vals") {
    compileCodeSnippet( """ object FinalVals {
                          |  private final val name = "sammy"
                          |  println(name)
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    // we should have 3 statements - initialising the val, executing println, and executing the parameter
    assertNMeasuredStatements(3)
  }

  test("scoverage should instrument selectors in match") {
    compileCodeSnippet( """ trait A {
                          |  def foo(a:String) = (if (a == "hello") 1 else 2) match {
                          |    case any => "yes"
                          |  }
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    // should instrument the method call, the if clause, thenp, thenp literal, elsep, elsep literal, case block,
    // case block literal
    assertNMeasuredStatements(8)
  }

  // https://github.com/scoverage/sbt-scoverage/issues/16
  test("scoverage should instrument for-loops but not the generated default case") {
    compileCodeSnippet( """ trait A {
                          |  def print1(list: List[String]) = for (string: String <- list) println(string)
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    assert(!reporter.hasWarnings)
    // should have one statement for the withFilter invoke, one of the match selector,
    // one of the case block, one for the case string RHS value, one for the foreach block.
    assertNMeasuredStatements(5)
  }

  test("scoverage should instrument val RHS") {
    compileCodeSnippet( """object A {
                          |  val name = BigDecimal(50.0)
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    assert(!reporter.hasWarnings)
    assertNMeasuredStatements(1)
  }

  test("scoverage should instrument all case statements in an explicit match") {
    compileCodeSnippet( """ trait A {
                          |  def foo(name: Any) = name match {
                          |    case i : Int => 1
                          |    case b : Boolean => 2
                          |    case _ => 3
                          |  }
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    assert(!reporter.hasWarnings)
    // should have one statement for each literal, one for each case block, and one for the selector.
    assertNMeasuredStatements(7)
  }

  test("scoverage should not instrument local macro implementation") {
    compileCodeSnippet( """
                          | object MyMacro {
                          | import scala.language.experimental.macros
                          | import scala.reflect.macros.Context
                          |  def test = macro testImpl
                          |  def testImpl(c: Context): c.Expr[Unit] = {
                          |    import c.universe._
                          |    reify {
                          |      println("macro test")
                          |    }
                          |  }
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    assertNoCoverage()
  }

  // https://github.com/skinny-framework/skinny-framework/issues/97
  test("scoverage should not instrument expanded macro code") {
    addToClassPath("org.slf4j", "slf4j-api", "1.7.7")
    addToClassPath("com.typesafe.scala-logging", "scala-logging-api_" + shortScalaVersion, "2.1.2")
    addToClassPath("com.typesafe.scala-logging", "scala-logging-slf4j_" + shortScalaVersion, "2.1.2")
    compileCodeSnippet( """import com.typesafe.scalalogging.slf4j.StrictLogging
                          |class MacroTest extends StrictLogging {
                          |  val name = "sammy"
                          |  logger.info("will break")
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
    assert(!reporter.hasWarnings)
    assertNoCoverage()
  }
}
