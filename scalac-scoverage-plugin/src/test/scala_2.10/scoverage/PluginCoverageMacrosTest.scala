package scoverage

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEachTestData, FunSuite, OneInstancePerTest}

/** @author Stephen Samuel */
class PluginCoverageMacrosTest
  extends FunSuite
  with MockitoSugar
  with OneInstancePerTest
  with BeforeAndAfterEachTestData {

  test("scoverage should skip macros") {
    val compiler = ScoverageCompiler.default
    val code = """
              import scala.language.experimental.macros
              import scala.reflect.macros.Context
              object Impl {
                def poly[T: c.WeakTypeTag](c: Context) = c.literal(c.weakTypeOf[T].toString)
              }

              object Macros {
                def poly[T] = macro Impl.poly[T]
              }"""
    compiler.compileCodeSnippet(code)
    assert(!compiler.reporter.hasErrors)
    compiler.assertNMeasuredStatements(0)
    compiler.assertNoCoverage()
  }

  test("plugin should not instrument local macro implementation") {
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
}



