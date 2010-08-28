package reaktor.scct

class ConstructorInstrumentationSpec extends InstrumentationSpec {

  "Constructor instrumentation" should instrument {
    "basic auxiliary constructors" in {
      offsetsMatch("class @Foo(x: Int) { def this(s: String) = @this(s.toInt) }")
    }
    "complicated auxiliary constructors" in {
      offsetsMatch("class @Foo(s: String) { def this(x: Int) = @this((x + x).toString) }")
    }
    "auxiliary constructors with functions" in {
      offsetsMatch("""|class @X(val s: String) {
                      |  def this(ii: Int) = @this((0 to ii).map(@_.toString).mkString("/"))
                      |}
                      |""".stripMargin)
    }
    "auxiliary constructors with functions, part II" in {
      offsetsMatch("""|class @X(val s: String) {
                      |  def this(ii: Int) = {
                      |    @this((0 to ii).map(cnt => { @println("yeah"); @cnt.toString + " : "}).mkString("/"))
                      |  }
                      |}
                      |""".stripMargin)
    }
    "extending class constructors" in {
      offsetsMatch("class @Foo(x: Int)\nclass @Bar(x: Int, y: Int) extends Foo(x)")
    }
    "multiline constructors" in {
      offsetsMatch("""|class @Example(val id: String) {
                      |  def this(id:String, other:String) = {
                      |    @this(id);
                      |    @setOther(other)
                      |  }
                      |  def setOther(s: String) {
                      |    @println(s);
                      |  }
                      |}""".stripMargin)
    }
    "pre-super vals in extends blocks" in {
      offsetsMatch("""|trait MyTrait { val x = @123 }
                      |class @MyClass extends { val xxx = { @println("buyakasha"); @List("s") }; var yyy = @123 } with MyTrait""".stripMargin)
    }
    "skip anonymous class constructor call" in {
      // Creation of the anonymous class also creates a default consructor for it, so skip instrumentation of that.
      offsetsMatch("class @Foo { val x = @new scala.collection.mutable.ArrayBuffer[String] { @this ++= List(\"wrok\") }; }")
    }
  }
}