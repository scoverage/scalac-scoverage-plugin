package scoverage

import munit.FunSuite

import java.io.File
import java.net.URLClassLoader

/** Runtime regression test for #680.
  *
  * The reporter's minimal case (github.com/josephlbarnett/testscoverageissue) passes under
  * `mvn verify` and throws under `mvn scoverage:report`:
  *
  *   java.lang.NullPointerException
  *     at com.example.Wrapper.equals(Wrapper.scala:13)  // obj.getClass != classOf[Wrapper]
  *     at com.example.Wrapper.equals(Wrapper.scala:21)  // other != null && ...
  *     at com.example.WrapperTest.testEquals(WrapperTest.scala:11)
  *
  * The two stacked `equals` frames are the symptom: under instrumentation `other != null` is
  * compiled as `other.equals(null)` rather than a reference check, so it recurses into the
  * overridden `equals` with a genuinely null argument and dereferences it.
  *
  * PluginCoverageTest checks statement counts after compilation. This test loads the class
  * scoverage just instrumented and calls `equals` through reflection, so it fails with the
  * actual NullPointerException rather than with a changed count.
  */
class Issue680RegressionTest extends FunSuite {

  // Same shape as the reporter's Wrapper.scala; the names differ only to avoid clashing with
  // other snippets compiled into the shared test output directory.
  private val snippet =
    """
      |package issue680
      |class Bean680 { var id: String = _ }
      |class Wrapper680 {
      |  var bean: Bean680 = _
      |  var code: String = _
      |  override def equals(obj: Any): Boolean = {
      |    if (obj.getClass != classOf[Wrapper680]) {
      |      return false
      |    } else {
      |      val other = obj.asInstanceOf[Wrapper680]
      |      if (
      |        (other.code != code) ||
      |        (other.bean == null && bean != null) ||
      |        (other.bean != null && bean == null) ||
      |        (other != null && bean != null && other.bean.id != bean.id)
      |      ) {
      |        return false
      |      } else {
      |        return true
      |      }
      |    }
      |    super.equals(obj)
      |  }
      |}
      |""".stripMargin

  test(
    "instrumented Wrapper680.equals mirrors the #680 repro sequence without NullPointerException"
  ) {
    val compiler = ScoverageCompiler.default
    compiler.compileCodeSnippet(snippet)
    compiler.assertNoErrors()

    val outDir = new File(compiler.settings.outdir.value)
    val loader =
      new URLClassLoader(Array(outDir.toURI.toURL), getClass.getClassLoader)
    val wrapperClass = loader.loadClass("issue680.Wrapper680")
    val beanClass = loader.loadClass("issue680.Bean680")

    def newWrapper(): AnyRef =
      wrapperClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    def newBean(): AnyRef =
      beanClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    def setBean(w: AnyRef, b: AnyRef): Unit =
      wrapperClass.getMethod("bean_$eq", beanClass).invoke(w, b)
    def setBeanId(b: AnyRef, id: String): Unit =
      beanClass.getMethod("id_$eq", classOf[String]).invoke(b, id)
    def equalsCall(a: AnyRef, b: AnyRef): Boolean =
      wrapperClass
        .getMethod("equals", classOf[Object])
        .invoke(a, b)
        .asInstanceOf[Boolean]

    // Step-by-step replay of WrapperTest.testEquals(). The *very first* call below is exactly
    // where the reporter's `mvn scoverage:report` run threw: both `bean` fields are still null
    // (Scala default), so evaluation of the `||` chain reaches the last clause and short-circuit
    // `&&` evaluates `other != null` first - the miscompiled comparison this test guards against.
    val a = newWrapper()
    val b = newWrapper()
    assert(
      equalsCall(a, b),
      "two fresh wrappers with null beans should be equal"
    )

    val beanA = newBean()
    setBean(a, beanA)
    assert(
      !equalsCall(a, b),
      "wrapper with a bean should not equal one without a bean"
    )

    val beanB = newBean()
    setBean(b, beanB)
    assert(
      equalsCall(a, b),
      "wrappers with two fresh (equal) beans should be equal again"
    )

    setBeanId(beanA, "id")
    assert(!equalsCall(a, b), "differing bean ids should not be equal")

    setBeanId(beanB, "id")
    assert(equalsCall(a, b), "matching bean ids should be equal again")
  }
}
