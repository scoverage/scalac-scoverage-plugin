package reaktor.scct

class StatementInstrumentationSpec extends InstrumentationSpec {
  "If instrumentation" should instrument {
    "hardcoded if's" in {
      classOffsetsMatch("if (true) @println(x)")
      defOffsetsMatch("if (true) @println(x)")
    }
    "not hardcoded false's" in {
      classOffsetsMatch("if (false) println(x)")
      defOffsetsMatch("if (false) println(x)")
    }
    "basic if's" in {
      classOffsetsMatch("if (x > 2) @println(x.toString)")
      classOffsetsMatch("if (x > 2) @println(x.toString) else @println(x.toString)")
      classOffsetsMatch("if (x > 2) { @999; @println(x.toString) } else { @888; @println(x.toString) }")
    }
    "conditional if necessary" in {
      classOffsetsMatch("if (Some(x).map(@_*10).map(@_ > 20).getOrElse(false)) @println(x.toString)")
    }
    "isInstanceOf in conditional" in {
      classOffsetsMatch("""def foo(z: Any) = if (z.isInstanceOf[String]) @"string" else @"not" """)
    }
    "many else's" in {
      classOffsetsMatch("""val z = if (x % 2 == 0) @"first" else if (x % 3 == 1) @"second" else @"third";""")
    }
  }

  "Return instrumentation" should instrument {
    "return in method" in {
      classOffsetsMatch("def foo(z: Int): Boolean = { @println(z.toString); @return z % 2 == 0 }")
    }

    "return from if" in {
      classOffsetsMatch("def foo(z: Int): Boolean = if (z % 2 == 0) @return true else @return false")
      classOffsetsMatch("def foo(z: Int): Boolean = if (z % 2 == 0) @return true else @false")
    }
    "return from case" in {
      classOffsetsMatch("def foo(z: Int): Boolean = z match { case 2 => @return true; case _ => @return false } ")
    }
  }

  "While/doWhile instrumentation" should instrument {
    "basic while" in {
      defOffsetsMatch("var z = @0; while (z < 5) @z += 1")
      defOffsetsMatch("var z = @0; while (z < 5) { @println(z); @z += 1 }")
    }
    "basic do - while" in {
      classOffsetsMatch("var z = @0; @do @z += 1 while (z < 5)")
      classOffsetsMatch("var z = @0; @do { @println(z); @z += 1 } while (z < 5)")
    }
  }

  "Match/case instrumentation" should instrument {
    "basic match" in {
      classOffsetsMatch("""val z = x match { case 1 => @"one"; case 2 => @"two"; case _ => @throw new Exception() }""")
    }
    "match with guard" in {
      classOffsetsMatch("""val z = x match { case 1 if (x % 2 == 0) => @"one"; case _ => @"other" }""")
    }
    "case sequence" in {
      classOffsetsMatch("""val z: Option[Int] => Int = { case Some(q) => @q; case None => @0 }""")
    }
  }

  "Exception instrumentation" should instrument {
    "basic try/catch/finally" in {
      defOffsetsMatch("try { @100 / x } catch { case e => @println(\"ouch\") } finally { @println(\"done\") }")
      defOffsetsMatch("try { @super.hashCode } catch { case _ => @throw new Exception() }")
    }
    "basic throw" in {
      classOffsetsMatch("@throw new Exception()")
      defOffsetsMatch("@throw new Exception()")
    }
  }

  "For instrumentation" should instrument {
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

  "Enumeration instrumentation" should instrument {
    "basic enumeration" in {
      // TODO: Not sure how to instrument these...
      offsetsMatch("object Enum extends Enumeration { val Black = @Value; val White = @Value }")
      offsetsMatch("object Enum extends Enumeration { val Red,Green,Blue = @@@Value }")
      offsetsMatch("""object Enum extends Enumeration { val Black = @Value("B"); val White = @Value("W") }""")
    }
  }

  "Implicit parameter instrumentation" should instrument {
    "basic implicit param usage" in {
      classOffsetsMatch("""implicit val defaultString = @"hi"; def say(implicit s: String) { @println(s) }""")
    }
  }

  "Instrumentation of other misc. bits and pieces" should instrument {
    "seq to var-args" in {
      classOffsetsMatch("val z = @Map(0 to(5) map { n => @(n.toString, n*5)} : _*)")
    }
    "symbols" in {
      classOffsetsMatch("val z = @'sym")
    }
    "list concatenation" in {
      // TODO: compiler doesn't have an offset prior to the :::
      defOffsetsMatch("List(1) @::: List(2)")
    }
  }

}