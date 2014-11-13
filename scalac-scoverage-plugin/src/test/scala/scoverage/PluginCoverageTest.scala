package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEachTestData, FunSuite, OneInstancePerTest}

/** @author Stephen Samuel */
class PluginCoverageTest
  extends FunSuite
  with MockitoSugar
  with OneInstancePerTest
  with BeforeAndAfterEachTestData {

  test("scoverage should instrument default arguments with methods") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """ object DefaultArgumentsObject {
                          |  val defaultName = "world"
                          |  def makeGreeting(name: String = defaultName): String = {
                          |    s"Hello, $name"
                          |  }
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    // we should have 2 statements - initialising the val and executing string sub in the def
    compiler.assertNMeasuredStatements(2)
  }

  test("scoverage should skip macros") {
    val compiler = ScoverageCompiler.default
    val code = """
              import scala.language.experimental.macros
              import scala.reflect.macros.Context
              class Impl(val c: Context) {
                import c.universe._
                def poly[T: c.WeakTypeTag] = c.literal(c.weakTypeOf[T].toString)
              }
              object Macros {
                def poly[T] = macro Impl.poly[T]
              }"""
    compiler.compileCodeSnippet(code)
    assert(!compiler.reporter.hasErrors)
    compiler.assertNMeasuredStatements(0)
  }

  test("scoverage should instrument final vals") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """ object FinalVals {
                          |  final val name = {
                          |     val name = "sammy"
                          |     if (System.currentTimeMillis() > 0) {
                          |      println(name)
                          |     }
                          |  }
                          |  println(name)
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    // we should have 3 statements - initialising the val, executing println, and executing the parameter
    compiler.assertNMeasuredStatements(8)
  }

  test("scoverage should instrument selectors in match") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """ trait A {
                          |  def foo(a:String) = (if (a == "hello") 1 else 2) match {
                          |    case any => "yes"
                          |  }
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    // should instrument the method call, the if clause, thenp, thenp literal, elsep, elsep literal, case block,
    // case block literal
    compiler.assertNMeasuredStatements(8)
  }

  // https://github.com/scoverage/sbt-scoverage/issues/16
  test("scoverage should instrument for-loops but not the generated default case") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """ trait A {
                          |  def print1(list: List[String]) = for (string: String <- list) println(string)
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have one statement for the withFilter invoke, one of the match selector,
    // one of the case block, one for the case string RHS value, one for the foreach block.
    compiler.assertNMeasuredStatements(5)
  }

  test("scoverage should instrument val RHS") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """object A {
                          |  val name = BigDecimal(50.0)
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    compiler.assertNMeasuredStatements(1)
  }

  test("scoverage should instrument all case statements in an explicit match") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """ trait A {
                          |  def foo(name: Any) = name match {
                          |    case i : Int => 1
                          |    case b : Boolean => 2
                          |    case _ => 3
                          |  }
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have one statement for each literal, one for each case block, and one for the selector.
    compiler.assertNMeasuredStatements(7)
  }

  test("scoverage should support yields") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """
                          |  object Yielder {
                          |    val holidays = for ( name <- Seq("sammy", "clint", "lee");
                          |                         place <- Seq("london", "philly", "iowa") ) yield {
                          |      name + " has been to " + place
                          |    }
                          |  }""".stripMargin)
    assert(!compiler.reporter.hasErrors)
    // 2 statements for the two applies in Seq, one for each literal which is 6, one for the flat map,
    // one for the map, one for the yield op.
    compiler.assertNMeasuredStatements(11)
  }

  test("scoverage should not instrument local macro implementation") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """
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
    assert(!compiler.reporter.hasErrors)
    compiler.assertNoCoverage()
  }

  // https://github.com/skinny-framework/skinny-framework/issues/97
  test("scoverage should not instrument expanded macro code") {
    val compiler = ScoverageCompiler.default
    compiler.addToClassPath("org.slf4j", "slf4j-api", "1.7.7")
    compiler
      .addToClassPath("com.typesafe.scala-logging",
        "scala-logging-api_" + ScoverageCompiler.ShortScalaVersion,
        "2.1.2")
    compiler
      .addToClassPath("com.typesafe.scala-logging",
        "scala-logging-slf4j_" + ScoverageCompiler.ShortScalaVersion,
        "2.1.2")
    compiler.compileCodeSnippet( """import com.typesafe.scalalogging.slf4j.StrictLogging
                          |class MacroTest extends StrictLogging {
                          |  logger.info("will break")
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    compiler.assertNoCoverage()
  }
}
