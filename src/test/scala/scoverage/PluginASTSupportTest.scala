package scoverage

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, OneInstancePerTest}

/** @author Stephen Samuel */
class PluginASTSupportTest extends FunSuite with MockitoSugar with OneInstancePerTest with PluginSupport {

  test("scoverage component should handle simple class") {
    compileCodeSnippet( """ class Test { val name = "sammy" } """)
  }

  test("scoverage component should handle macro") {
    compileCodeSnippet( """ class Test { val name = "sammy" } """)
  }
}

//
//object MyMacro {
//  def test = macro testImpl
//  def testImpl(c: Context): c.Expr[Unit] = {
//    import c.universe._
//    reify {
//      println("macro test")
//    }
//  }
//}
