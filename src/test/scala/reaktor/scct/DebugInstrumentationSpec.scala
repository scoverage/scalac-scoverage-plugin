package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {
  override def debug = true

  /*
    Doesn't instrument the cases,
    For 2.10, typer-phase generates the following for the collect statement:

    def collect(x: String): String = scala.this.Predef.augmentString(x).collect[Char, String](({
      @SerialVersionUID(0) final <synthetic> class $anonfun extends scala.runtime.AbstractPartialFunction[Char,Char] with Serializable {
        def <init>(): anonymous class $anonfun = {
          $anonfun.super.<init>();
          ()
        };
        final override def applyOrElse[A1 >: Nothing <: Char, B1 >: Char <: Any](x$1: A1, default: A1 => B1): B1 = (x$1: A1 @unchecked) match {
          case 'a' => 'a'
        };
        final def isDefinedAt(x$1: Char): Boolean = (x$1: Char @unchecked) match {
          case 'a' => true
        }
      };
      new anonymous class $anonfun()
    }: PartialFunction[Char,Char]))(scala.this.Predef.StringCanBuildFrom);
  */
  /*
  "literal in partial functions" in {
    val blocks = compileToData("""|class Foo {
                                  |  def collect(x: String): String = x.collect {
                                  |    case 'a' => @'a'
                                  |  }
                                  |}""".stripMargin)
    println(blocks)
    1 mustEqual 1
  }
  */

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

  /*
    This can't work until plugin is moved after uncurry.
    The openOr(_0_) gets changed into an anonymous function on uncurry.
   */
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

  /*
    typer inlines final vals without a type, but the "getter" gets instrumented,
    so showing red for final val x = 5.
   */
  /*
  "final val without type" in {
    offsetsMatch("""|final val x = @5
                    |println(@x + 5)
                    |}""".stripMargin)
  }
   */
}