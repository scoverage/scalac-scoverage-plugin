package scoverage

import org.scalatest.BeforeAndAfterEachTestData
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite

/** https://github.com/scoverage/scalac-scoverage-plugin/issues/196
  */
class PluginCoverageScalaJsTest
    extends AnyFunSuite
    with OneInstancePerTest
    with BeforeAndAfterEachTestData
    with MacroSupport {

  ignore("scoverage should ignore default undefined parameter") {
    val compiler = ScoverageCompiler.default
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
