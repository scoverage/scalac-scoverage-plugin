package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest._

/** @author Stephen Samuel */
class PluginASTSupportTest
  extends FunSuite
  with MockitoSugar
  with OneInstancePerTest
  with PluginSupport
  with BeforeAndAfterEachTestData {

  override protected def afterEach(testData: TestData): Unit = {
    assert(!reporter.hasErrors)
  }

  test("scoverage component should handle simple class") {
    compileCodeSnippet( """ class Test { val name = "sammy" } """)
    assert(!reporter.hasErrors)
  }

  test("scoverage component should handle basic macros") {
    compileCodeSnippet( """ import scala.language.experimental.macros
                          | import scala.reflect.macros.Context
                          |
                          | object MyMacro {
                          |  def test = macro testImpl
                          |  def testImpl(c: Context): c.Expr[Unit] = {
                          |    import c.universe._
                          |    reify {
                          |      println("macro test")
                          |    }
                          |  }
                          |} """.stripMargin)
    assert(!reporter.hasErrors)
  }
}


