package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {
  override def debug = false

  /* works: */
  "extending java classes" in {
    offsetsMatch("""|class Foo @extends java.util.ArrayList[String] {
                    |  override def size = @12
                    |}""".stripMargin)
  }

  /*
    Doesn't work, the 'def size' hits this and stops instrumentation:
      case dd: DefDef if isInAnonymousClass => (false, t)
  */
  /*
  "anonymously extending java classes" in {
    offsetsMatch("""|class Foo @{
                    |  val aa = @new java.util.ArrayList[String]() {
                    |    override def size = {
                    |      @println("first.")
                    |      @12
                    |    }
                    |  }
                    |}""".stripMargin)

    }
   */

  /* This can't work until plugin is moved after uncurry: */

  /*
  "Literal as def parameter with type () => T" in {
    offsetsMatch("""|
                    |final class Full[+A] {
                    |  def openOr[B >: A](default: => B): B = @default
                    |}
                    |
                    |class Foo @{
                    |  val x = Full@[Int]()
                    |  @x.openOr(@0)
                    |}""".stripMargin)
  }
  */
}