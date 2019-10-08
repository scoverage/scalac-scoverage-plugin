package scoverage

import org.scalatest.{ BeforeAndAfterEachTestData, FunSuite, OneInstancePerTest }

/**
 * https://github.com/scoverage/scalac-scoverage-plugin/issues/196
 */
class PluginCoverageScalaJsTest
  extends FunSuite
  with OneInstancePerTest
  with BeforeAndAfterEachTestData
  with MacroSupport {

  test("scoverage should ignore default undefined parameter") {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(
      """import scala.scalajs.js
        |
        |object JSONHelper {
        |  def toJson(value: String): String = js.JSON.stringify(value)
        |}""".stripMargin)
    assert(!compiler.reporter.hasErrors)
    compiler.assertNMeasuredStatements(4)
  }
}
