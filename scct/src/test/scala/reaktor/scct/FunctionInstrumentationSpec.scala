package reaktor.scct

class FunctionInstrumentationSpec extends InstrumentationSpec {

  "basic function instrumentation" should instrument {
    "one-liners" in {
      classOffsetsMatch("(@1 to 10).foreach(x => @println(x))")
    }
    "blocks" in {
      classOffsetsMatch("(@1 to 10).foreach { x => { @println(x); @println(x) } }")
    }
    "functions with _" in {
      classOffsetsMatch("(@1 to 10).foreach(@println _)")
    }
    "even multiple blocks" in {
      classOffsetsMatch("""|@Some("foo").map { x =>
                           |  @println("map")
                           |  @"map"
                           |}.orElse {
                           |  @println("else")
                           |  @Some("else")
                           |}""".stripMargin)
    }
  }
}