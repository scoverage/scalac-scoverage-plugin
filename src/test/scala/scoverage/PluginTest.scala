package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, OneInstancePerTest}

/** @author Stephen Samuel */
class PluginTest extends FunSuite with MockitoSugar with OneInstancePerTest with PluginSupport {

  test("scoverage component should handle simple class") {
    compileCodeSnippet( """ class Test { val name = "sammy" } """)
  }
}
