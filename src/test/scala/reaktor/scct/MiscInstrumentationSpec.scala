package reaktor.scct

class MiscInstrumentationSpec extends InstrumentationSpec {

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

  "direct match statement in def" in {
    classOffsetsMatch(
      """|def matchCase(ii: Int): Tuple2[Boolean,Int] = @ii match {
         |  case _ if (@System.currentTimeMillis % 2 == 0) => @(true, ii)
         |  case 2 => @(true, 2)
         |  case _ => @(false, ii)
         |}
         |""".stripMargin)
  }

  "weird case class in class offset" in {
    offsetsMatch("class Foo @{ case class Bar @}") // TODO: how is the offset there?
  }

  "some+map" in {
    classOffsetsMatch("""val z = @Some(x).map(@_ * 1000) match { case Some(1) => @"one"; case _ => }""")
  }

  "extending java classes" in {
    offsetsMatch("""|class Foo @extends java.util.ArrayList[String] {
                    |  override def size = @12
                    |}""".stripMargin)
  }


}
