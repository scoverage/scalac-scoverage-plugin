package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {
  override def debug = false

  "match/case" in {
    classOffsetsMatch("""|var ii = @12
                        |@ii match {
                        |  case 1 => @println("pow"); @"one"
                        |  case 2 => @"two"
                        |  case _ => @"many"
                        |}""".stripMargin)
  }

  "match/case with if" in {
    classOffsetsMatch("""|var ii = @12
                        |@ii match {
                        |  case 1 if (@System.currentTimeMillis > 1) => @"yeah"
                        |  case _ => @"nope"
                        |}""".stripMargin)
  }
  "pow" in {
    offsetsMatch("class Foo @{ case class Bar @}") // TODO: how is the offset there?
    classOffsetsMatch("""val z = @Some(x).map(@_ * 1000) match { case Some(1) => @"one"; case _ => }""")
  }

  /* works: */
  "extending java classes" in {
    offsetsMatch("""|class Foo @extends java.util.ArrayList[String] {
                    |  override def size = @12
                    |}""".stripMargin)
  }

  /* don't know:
  "literal in partial functions" in {
    val blocks = compileToData("""|class Foo {
                                  |  def collect(x: String): String = x.collect {
                                  |    case 'a' => 'a'
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