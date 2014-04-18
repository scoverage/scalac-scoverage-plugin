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

  test("scoverage component should ignore basic macros") {
    compileCodeSnippet( """
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
    assert(!reporter.hasErrors)
  }

  test("scoverage component should ignore complex macros #11") {
    compileCodeSnippet( """ object ComplexMacro {
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
    assert(!reporter.hasErrors)
  }

//  test("scoverage supports skinny #23") {
//    addToClassPath("org.joda", "joda-convert", "1.3.1")
//    addToClassPath("joda-time", "joda-time", "2.3")
//    addToClassPath("org.scalikejdbc", "scalikejdbc_2.10", "1.7.5")
//    addToClassPath("org.scalikejdbc", "scalikejdbc-interpolation_2.10", "1.7.5")
//    addToClassPath("org.scalikejdbc", "scalikejdbc-interpolation-core_2.10", "1.7.5")
//    addToClassPath("org.scalikejdbc", "scalikejdbc-interpolation-macro_2.10", "1.7.5")
//    addToClassPath("org.scalikejdbc", "scalikejdbc-config_2.10", "1.7.5")
//    addToClassPath("org.skinny-framework", "skinny-common_2.10", "1.0.8")
//    addToClassPath("org.skinny-framework", "skinny-framework_2.10", "1.0.8")
//    addToClassPath("org.skinny-framework", "skinny-orm_2.10", "1.0.8")
//    addToClassPath("org.slf4j", "slf4j-api", "1.7.7")
//    compileSourceResources(getClass.getResource("/scoverage/skinny/Member.scala"))
//    assert(!reporter.hasErrors)
//  }
}

