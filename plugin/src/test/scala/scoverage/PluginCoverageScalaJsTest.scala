package scoverage

import munit.FunSuite

/** https://github.com/scoverage/scalac-scoverage-plugin/issues/196
  */
class PluginCoverageScalaJsTest extends FunSuite with MacroSupport {

  test("scoverage should ignore default undefined parameter") {
    val compiler = ScoverageCompiler.defaultJS
    compiler.compileCodeSnippet(
      """import scala.scalajs.js
        |
        |object JSONHelper {
        |  def toJson(value: String): String = js.JSON.stringify(value)
        |}""".stripMargin
    )
    assert(!compiler.reporter.hasErrors)
    compiler.assertNMeasuredStatements(4)
  }
}
