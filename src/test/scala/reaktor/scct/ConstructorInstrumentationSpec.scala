package reaktor.scct

class ConstructorInstrumentationSpec extends InstrumentationSpec {

  "Constructor instrumentation" should {
    "basic auxiliary constructors" in {
      offsetsMatch("class Foo@(x: Int) { def this(s: String) = @this(s.toInt) }")
    }
    "complicated auxiliary constructors" in {
      offsetsMatch("class Foo@(s: String) { def this(x: Int) = @this((x + x).toString) }")
    }
    "auxiliary constructors with functions" in {
      offsetsMatch("""|class X@(val s: String) {
                      |  def this(ii: Int) = @this((0 to ii).map(@_.toString).mkString("/"))
                      |}
                      |""".stripMargin)
    }
    "auxiliary constructors with functions, part II" in {
      offsetsMatch("""|class X@(val s: String) {
                      |  def this(ii: Int) = {
                      |    @this((0 to ii).map(cnt => { @println("yeah"); @cnt.toString + " : "}).mkString("/"))
                      |  }
                      |}
                      |""".stripMargin)
    }
    "extending class constructors" in {
      offsetsMatch("class Foo@(x: Int)\nclass Bar@(x: Int, y: Int) extends Foo(x)")
    }
    "multiline constructors" in {
      offsetsMatch("""|class Example@(val id: String) {
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
                      |class MyClass @extends { val xxx = { @println("buyakasha"); @List("s") }; var yyy = @123 } with MyTrait""".stripMargin)
    }
    "skip anonymous class constructor call" in {
      // Creation of the anonymous class also creates a default consructor for it, so skip instrumentation of that.
      offsetsMatch("class Foo @{ val x = @new scala.collection.mutable.ArrayBuffer[String] { @this ++= List(\"wrok\") }; }")
    }
  }

  "Not messing about with the super call in constructors should work for" should {
    "basic extending constructor" in {
      val src = "class Obj(y: String)"
      val stats = digOutConstructorStats(compileSource(src))
      stats.length mustEqual 2
      stats(0) mustEqual "Obj.super.this()"
      stats(1) must startWith("reaktor.scct.Coverage.invoked(")
    }
    "basic extending constructor" in {
      val src = "class Second(y: String) extends First(y); class First(x: String)"
      val stats = digOutConstructorStats(compileSource(src))
      stats.length mustEqual 2
      stats(0) mustEqual "Second.super.this(y)"
      stats(1) must startWith("reaktor.scct.Coverage.invoked(")
    }
    "extension override constructors" in {
      val src = "class Second extends { val x = 1; val y = 2 } with First; trait First"
      val stats = digOutConstructorStats(compileSource(src))
      stats.length mustEqual 4
      stats(0) must startWith("val x: Int = ")
      stats(1) must startWith("val y: Int = ")
      stats(2) must startWith("Second.super.this()")
      stats(3) must startWith("reaktor.scct.Coverage.invoked(")
    }
    "curried constructors" in {
      val src = "case class Second(p12: String, p22: String) extends First(p12)(p22); class First(p1: String)(p2: String)"
      val stats = digOutConstructorStats(compileSource(src))
      stats.length mustEqual 2
      stats(0) mustEqual "Second.super.this(p12)(p22)"
      stats(1) must startWith("reaktor.scct.Coverage.invoked(")
    }
  }

  private def digOutConstructorStats(runner: PluginRunner): List[String] = {
    import runner._
    val y: Tree = runner.currentRun.units.next.body
    val PackageDef(_, classes) = y
    val ClassDef(_, _, _, Template(_, _, content)) = classes.head
    val Some(DefDef(_,_,_,_,_,rhs)) = content.find(x => x.isInstanceOf[DefDef] && x.asInstanceOf[DefDef].name.toString == "<init>")
    val Block(stats, _) = rhs
    stats.map(_.toString)
  }

}