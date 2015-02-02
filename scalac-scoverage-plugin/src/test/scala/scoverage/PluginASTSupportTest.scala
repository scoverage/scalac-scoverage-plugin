package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest._

/** @author Stephen Samuel */
class PluginASTSupportTest
  extends FunSuite
  with MockitoSugar
  with OneInstancePerTest
  with BeforeAndAfterEachTestData {

  override protected def afterEach(testData: TestData): Unit = {
    val compiler = ScoverageCompiler.default
    assert(!compiler.reporter.hasErrors)
  }

  test("scoverage component should ignore basic macros") {
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
  }

  test("scoverage component should ignore complex macros #11") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """ object ComplexMacro {
                          |
                          |  import scala.language.experimental.macros
                          |  import scala.reflect.macros.Context
                          |
                          |  def debug(params: Any*) = macro debugImpl
                          |
                          |  def debugImpl(c: Context)(params: c.Expr[Any]*) = {
                          |    import c.universe._
                          |
                          |    val trees = params map {param => (param.tree match {
                          |      case Literal(Constant(_)) => reify { print(param.splice) }
                          |      case _ => reify {
                          |        val variable = c.Expr[String](Literal(Constant(show(param.tree)))).splice
                          |        print(s"$variable = ${param.splice}")
                          |      }
                          |    }).tree
                          |    }
                          |
                          |    val separators = (1 until trees.size).map(_ => (reify { print(", ") }).tree) :+ (reify { println() }).tree
                          |    val treesWithSeparators = trees zip separators flatMap {p => List(p._1, p._2)}
                          |
                          |    c.Expr[Unit](Block(treesWithSeparators.toList, Literal(Constant(()))))
                          |  }
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
  }

 // https://github.com/scoverage/scalac-scoverage-plugin/issues/32
  test("exhaustive warnings should not be generated for @unchecked") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """object PartialMatchObject {
                          |  def partialMatchExample(s: Option[String]): Unit = {
                          |    (s: @unchecked) match {
                          |      case Some(str) => println(str)
                          |    }
                          |  }
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
  }

  // https://github.com/skinny-framework/skinny-framework/issues/97
  test("macro range positions should not break plugin") {
    val compiler = ScoverageCompiler.default
    compiler.addToClassPath("org.slf4j", "slf4j-api", "1.7.7")
    compiler.addToClassPath("com.typesafe.scala-logging", "scala-logging-api_" + ScoverageCompiler.ShortScalaVersion, "2.1.2")
    compiler.addToClassPath("com.typesafe.scala-logging", "scala-logging-slf4j_" + ScoverageCompiler.ShortScalaVersion, "2.1.2")
    compiler.compileCodeSnippet( """import com.typesafe.scalalogging.slf4j.StrictLogging
                          |
                          |object MacroTest extends StrictLogging {
                          |  println("Hello")
                          |  logger.info("will break")
                          |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
  }

  // https://github.com/scoverage/scalac-scoverage-plugin/issues/45
  test("compile final vals in annotations") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """object Foo  {
                          |  final val foo = 1L
                          |}
                          |@SerialVersionUID(value = Foo.foo)
                          |class Bar
                          |""".stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
  }

  test("type param with default arg supported") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet( """class TypeTreeObjects {
                                   |  class Container {
                                   |    def typeParamAndDefaultArg[C](name: String = "sammy"): String = name
                                   |  }
                                   |  new Container().typeParamAndDefaultArg[Any]()
                                   |} """.stripMargin)
    assert(!compiler.reporter.hasErrors)
    assert(!compiler.reporter.hasWarnings)
  }
}

