package scoverage

import munit.FunSuite

/** @author Stephen Samuel */
class PluginCoverageTest extends FunSuite with MacroSupport {

  test("scoverage should instrument default arguments with methods") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """ object DefaultArgumentsObject {
        |  val defaultName = "world"
        |  def makeGreeting(name: String = defaultName): String = {
        |    "Hello, " + name
        |  }
        |} """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    // we expect:
    // instrumenting the default-param which becomes a method call invocation
    // the method makeGreeting is entered.
    compiler.assertNMeasuredStatements(2)
  }

  test("scoverage should skip macros") {
    val compiler = ScoverageCompiler.default
    val code =
      if (ScoverageCompiler.ShortScalaVersion == "2.10")
        """
              import scala.language.experimental.macros
              import scala.reflect.macros.Context
              object Impl {
                def poly[T: c.WeakTypeTag](c: Context) = c.literal(c.weakTypeOf[T].toString)
              }

              object Macros {
                def poly[T]: String = macro Impl.poly[T]
              }"""
      else
        s"""
              import scala.language.experimental.macros
              import scala.reflect.macros.blackbox.Context
              class Impl(val c: Context) {
                import c.universe._
                def poly[T: c.WeakTypeTag] = q"$${c.weakTypeOf[T].toString}"
              }
              object Macros {
                def poly[T]: String = macro Impl.poly[T]
              }"""
    compiler.compileCodeSnippet(code)
    assert(!compiler.reporter.hasErrors)
    compiler.assertNMeasuredStatements(0)
  }

  test("scoverage should instrument final vals") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" object FinalVals {
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

  test("scoverage should not instrument the match as a statement") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" object A {
                                  |    System.currentTimeMillis() match {
                                  |      case x => println(x)
                                  |    }
                                  |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)

    /** should have the following statements instrumented:
      * the selector, clause/skip 1
      */
    compiler.assertNMeasuredStatements(3)
  }
  test("scoverage should instrument match guards") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" object A {
                                  |    System.currentTimeMillis() match {
                                  |      case l if l < 1000 => println("a")
                                  |      case l if l > 1000 => println("b")
                                  |      case _ => println("c")
                                  |    }
                                  |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)

    /** should have the following statements instrumented:
      * the selector, guard 1, clause 1, guard 2, clause 2, clause 3
      */
    compiler.assertNMeasuredStatements(9)
  }

  test("scoverage should instrument non basic selector") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" trait A {
                                  |  def someValue = "sammy"
                                  |  def foo(a:String) = someValue match {
                                  |    case any => "yes"
                                  |  }
                                  |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    // should instrument:
    // the someValue method entry
    // the selector call
    // case block "yes" literal
    // skip case block
    compiler.assertNMeasuredStatements(4)
  }

  test("scoverage should instrument conditional selectors in a match") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """ trait A {
        |  def foo(a:String) = (if (a == "hello") 1 else 2) match {
        |    case any => "yes"
        |  }
        |} """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    // should instrument:
    // the if clause,
    // then block,
    // then literal "1",
    // else block,
    // else literal "2",
    // case block "yes" literal
    // skip case block "yes" literal
    compiler.assertNMeasuredStatements(7)
  }

  test(
    "scoverage should instrument anonymous function with pattern matching body"
  ) {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """ object A {
        |  def foo(a: List[Option[Int]]) = a.map {
        |    case Some(value) => value + 1
        |    case None => 0
        |  }
        |} """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    // should instrument:
    // the def method entry,
    // case Some,
    // case block expression
    // case none,
    // case block literal "0"

    // account for canbuildfrom statement
    val expectedStatementsCount =
      if (ScoverageCompiler.ShortScalaVersion < "2.13") 6 else 5
    compiler.assertNMeasuredStatements(expectedStatementsCount)
  }

  // https://github.com/scoverage/sbt-scoverage/issues/16
  test(
    "scoverage should instrument for-loops but not the generated scaffolding"
  ) {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """ trait A {
        |  def print1(list: List[String]) = for (string: String <- list) println(string)
        |} """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should instrument:
    // the def method entry
    // foreach body
    // specifically we want to avoid the withFilter partial function added by the compiler
    compiler.assertNMeasuredStatements(2)
  }

  test("scoverage should instrument for-loop guards") {
    val compiler = ScoverageCompiler.default

    compiler.compileCodeSnippet(
      """object A {
        |  def foo(list: List[String]) = for (string: String <- list if string.length > 5)
        |    println(string)
        |} """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should instrument:
    // foreach body
    // the guard
    // but we want to avoid the withFilter partial function added by the compiler
    compiler.assertNMeasuredStatements(3)
  }

  test(
    "scoverage should correctly handle new with args (apply with list of args)"
  ) {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" object A {
                                  |  new String(new String(new String))
                                  | } """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have 3 statements, one for each of the nested strings
    compiler.assertNMeasuredStatements(3)
  }

  test(
    "scoverage should correctly handle no args new (apply, empty list of args)"
  ) {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" object A {
                                  |  new String
                                  | } """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have 1. the apply that wraps the select.
    compiler.assertNMeasuredStatements(1)
  }

  test("scoverage should correctly handle new that invokes nested statements") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """
        | object A {
        |  val value = new java.util.concurrent.CountDownLatch(if (System.currentTimeMillis > 1) 5 else 10)
        | } """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have 6 statements - the apply/new statement, two literals, the if cond, if elsep, if thenp
    compiler.assertNMeasuredStatements(6)
  }

  test("scoverage should instrument val RHS") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet("""object A {
                                  |  val name = BigDecimal(50.0)
                                  |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    compiler.assertNMeasuredStatements(1)
  }

  test("scoverage should not instrument function tuple wrapping") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """
        |    sealed trait Foo
        |    case class Bar(s: String) extends Foo
        |    case object Baz extends Foo
        |
        |    object Foo {
        |      implicit val fooOrdering: Ordering[Foo] = Ordering.fromLessThan {
        |        case (Bar(_), Baz) => true
        |        case (Bar(a), Bar(b)) => a < b
        |        case (_, _) => false
        |      }
        |    }
      """.stripMargin
    )

    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have 7 profiled statements: the outer apply, and three pairs of case patterns & blocks
    // we are testing that we don't instrument the tuple2 call used here
    compiler.assertNMeasuredStatements(7)
  }

  test("scoverage should instrument all case statements in an explicit match") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(""" trait A {
                                  |  def foo(name: Any) = name match {
                                  |    case i : Int => 1
                                  |    case b : Boolean => println("boo")
                                  |    case _ => 3
                                  |  }
                                  |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    // should have one statement for each case body
    // and one statement for each case skipped
    // selector is a constant so would be ignored.
    compiler.assertNMeasuredStatements(6)
  }

  test("plugin should support yields") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """
        |  object Yielder {
        |    val holidays = for ( name <- Seq("sammy", "clint", "lee");
        |                         place <- Seq("london", "philly", "iowa") ) yield {
        |      name + " has been to " + place
        |    }
        |  }""".stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    // 2 statements for the two applies in Seq, one for each literal which is 6, one for the operation passed to yield.
    // Depending on the collections api version, there can be additional implicit canBuildFrom statements.
    val expectedStatementsCount =
      if (ScoverageCompiler.ShortScalaVersion < "2.13") 11 else 9
    compiler.assertNMeasuredStatements(expectedStatementsCount)
  }

  test("plugin should not instrument local macro implementation") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(s"""
                                   | object MyMacro {
                                   | import scala.language.experimental.macros
                                   | import ${macroContextPackageName}.Context
                                   |  def test: Unit = macro testImpl
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

  test(
    "plugin should not instrument expanded macro code http://github.com/skinny-framework/skinny-framework/issues/97".ignore
  ) {
    val compiler = ScoverageCompiler.default
    macroSupportDeps.foreach(compiler.addToClassPath(_))
    compiler.compileCodeSnippet(s"""import scoverage.macrosupport.Tester
                                   |
                                   |class MacroTest {
                                   |  Tester.test
                                   |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    compiler.assertNoCoverage()
  }

  test(
    "plugin should handle return inside catch github.com/scoverage/scalac-scoverage-plugin/issues/93".ignore
  ) {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """
        |    object bob {
        |      def fail(): Boolean = {
        |        try {
        |          true
        |        } catch {
        |          case _: Throwable =>
        |            Option(true) match {
        |              case Some(bool) => return recover(bool) // comment this return and instrumentation succeeds
        |              case _ =>
        |            }
        |            false
        |        }
        |      }
        |      def recover(it: Boolean): Boolean = it
        |    }
      """.stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
    compiler.assertNMeasuredStatements(11)
  }
}
