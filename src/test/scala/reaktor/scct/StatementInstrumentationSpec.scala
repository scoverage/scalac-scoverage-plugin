package reaktor.scct

class StatementInstrumentationSpec extends InstrumentationSpec {
  "If instrumentation" should {
    "hardcoded if's" in {
      classOffsetsMatch("if (@true) @println(x)")
      defOffsetsMatch("if (@true) @println(x)")
    }
    "not hardcoded false's" in {
      classOffsetsMatch("if (@false) @println(x)")
      defOffsetsMatch("if (@false) @println(x)")
    }
    "basic if's" in {
      classOffsetsMatch("if (@x > 2) @println(x.toString)")
      classOffsetsMatch("if (@x > 2) @println(x.toString) else @println(x.toString)")
      classOffsetsMatch("if (@x > 2) { @999; @println(x.toString) } else { @888; @println(x.toString) }")
    }
    "conditional if necessary" in {
      classOffsetsMatch("if (@Some(x).map(@_*10).map(@_ > 20).getOrElse(false)) @println(x.toString)")
    }
    "isInstanceOf in conditional" in {
      classOffsetsMatch("""def foo(z: Any) = if (@z.isInstanceOf[String]) @"string" else @"not" """)
    }
    "many else's" in {
      classOffsetsMatch("""val z = if (@x % 2 == 0) @"first" else if (@x % 3 == 1) @"second" else @"third";""")
    }
  }

  "Return instrumentation" should {
    "return in method" in {
      classOffsetsMatch("def foo(z: Int): Boolean = { @println(z.toString); @return z % 2 == 0 }")
    }

    "return from if" in {
      classOffsetsMatch("def foo(z: Int): Boolean = if (@z % 2 == 0) @return true else @return false")
      classOffsetsMatch("def foo(z: Int): Boolean = if (@z % 2 == 0) @return true else @false")
    }
    "return from case" in {
      classOffsetsMatch("def foo(z: Int): Boolean = @z match { case 2 => @return true; case _ => @return false } ")
    }
  }

  "While/doWhile instrumentation" should {
    "basic while" in {
      classOffsetsMatch("var z = @0; while (@z < 5) @z += 1")
      classOffsetsMatch("var z = @0; while (@z < 5) { @println(z); @z += 1 }")
      defOffsetsMatch("var z = @0; while (@z < 5) @z += 1")
      defOffsetsMatch("var z = @0; while (@z < 5) { @println(z); @z += 1 }")
    }
    "basic do - while" in {
      classOffsetsMatch("var z = @0; do @z += 1 while (@z < 5)")
      classOffsetsMatch("var z = @0; do { @println(z); @z += 1 } while (@z < 5)")
    }
  }

  "Match/case instrumentation" should {
    "basic match" in {
      classOffsetsMatch("""val z = @x match { case 1 => @"one"; case 2 => @"two"; case _ => @throw new Exception() }""")
    }
    "matching result of expression" in {
      classOffsetsMatch("""val z = @Some(x).map(@_ * 1000) match { case Some(1) => @"one"; case _ => }""")
    }

    "match with guard" in {
      classOffsetsMatch("""val z = @x match { case 1 if (@x % 2 == 0) => @"one"; case _ => @"other" }""")
    }
    "case sequence" in {
      classOffsetsMatch("""val z: Option[Int] => Int = @{ case Some(q) => @q; case None => @0 }""")
    }
  }

  "Exception instrumentation" should {
    "basic try/catch/finally" in {
      defOffsetsMatch("try { @100 / x } catch { case e:Throwable => @println(\"ouch\") } finally { @println(\"done\") }")
      defOffsetsMatch("try { @super.hashCode } catch { case _:Throwable => @throw new Exception() }")
    }
    "basic throw" in {
      classOffsetsMatch("@throw new Exception()")
      defOffsetsMatch("@throw new Exception()")
    }
  }

  "For instrumentation" should {
    "basic for" in {
      classOffsetsMatch("val z = for (@i <- 0.to(10)) @println(i.toString)")
    }
    "for with yield" in {
      classOffsetsMatch("val z = for (@i <- 0.to(10)) yield @i*10")
      classOffsetsMatch("val z = for (@i <- 0.to(10)) yield { @println(i); @i*10 }")
    }
    "for-yield with guard" in {
      classOffsetsMatch("val z = for (@i <- List.range(0, x) if @i % 2 == 0) yield @i")
    }
    "multi-variable for" in {
      classOffsetsMatch("for (@i <- Iterator.range(0, x); @j <- Iterator.range(i + 1, x) if @i + j == 32) @println((i+j).toString)")
    }
  }

  "Enumeration instrumentation" should {
    "basic enumeration" in {
      // TODO: Not sure how to instrument these...
      offsetsMatch("object Enum extends Enumeration { val Black = @Value; val White = @Value }")
      offsetsMatch("object Enum extends Enumeration { val Red,Green,Blue = @@@Value }")
      offsetsMatch("""object Enum extends Enumeration { val Black = @Value("B"); val White = @Value("W") }""")
    }
  }

  "Implicit parameter instrumentation" should {
    "basic implicit param usage" in {
      classOffsetsMatch("""implicit val defaultString = @"hi"; def say(implicit s: String) { @println(s) }""")
    }
  }

  "Instrumentation of other misc. bits and pieces" should {
    "seq to var-args" in {
      classOffsetsMatch("val z = @Map(0 to(5) map { n => @(n.toString, n*5)} : _*)")
    }
    "symbols" in {
      classOffsetsMatch("val z = @'sym")
    }
    "list concatenation" in {
      // TODO: can't find an offset prior to the :::
      defOffsetsMatch("List(1) @::: List(2)")
    }
    "variable incrementation" in {
      classOffsetsMatch("var z = @0; @z += 1;")
    }
    "function block that doesn't use input" in {
      classOffsetsMatch("@Some(\"foo\").map(@System.getProperty)")
      defOffsetsMatch("@Some(\"foo\").map(@System.getProperty)")
    }
    "statements using mutable state" in {
      classOffsetsMatch("""|val z = @new scala.collection.mutable.ArrayBuffer[String]()
                           |@z ++= List("wrok")""".stripMargin)
    }
    "not instrument when compiler takes shortcuts" in {
      // Pre-scala 2.9.1, only block (the RHS of def) got instrumented.
      // scala 2.9.1+ instruments inside the block.
      classOffsetsMatch("def junk = { val i = @1; val j = @i; @j; }");
    }
    "compile jestan's case: case class constructor in package object called from separate package" in {
      offsetsMatch(
        """|package foo {
           |  package object common {
           |    case class Person@()
           |  }
           |}
           |
           |package bar {
           |  import foo.common._
           |  object myRequestVar extends RequestVar[Person](Person())
           |
           |}
           |
           |abstract class RequestVar[T]@(dflt: => T) {}
           |
           |""".stripMargin)
    }
  }

}