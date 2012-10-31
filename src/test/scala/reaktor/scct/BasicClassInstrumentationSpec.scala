package reaktor.scct

class BasicClassInstrumentationSpec extends InstrumentationSpec {
  "Basic template instrumentation" should {
    "basic class" in {
      offsetsMatch("class Foo@ ")
      offsetsMatch("class Foo @{}")
      offsetsMatch("class Foo@(x: Int) {}")
      offsetsMatch("class Foo@(val x: Int) {}")
      offsetsMatch("class Foo@(var x: Int) {}")
    }
    "sealed class" in {
      offsetsMatch("sealed class Foo @{}")
      offsetsMatch("sealed class Foo@(x: Int) {}")
      offsetsMatch("sealed class Foo@(val x: Int) {}")
    }
    "basic case class" in {
      offsetsMatch("case class Foo @{}")
      offsetsMatch("case class Foo@(x: Int) {}")
    }
    "sealed case class" in {
      offsetsMatch("sealed case class Foo @{}")
      offsetsMatch("sealed case class Foo@(x: Int) {}")
    }
    "basic object" in {
      offsetsMatch("object Foo {}")
    }
    "basic case object" in {
      offsetsMatch("case object Foo {}")
    }
    "annotation" in {
      offsetsMatch("class MyAnnotation @extends scala.annotation.StaticAnnotation")
    }
  }

  "Trait instrumentation" should {
    "not instrument empty trait" in {
      offsetsMatch("trait Foo {}")
    }
    "instrument trait body content" in {
      offsetsMatch("trait Foo { @println(\"jeah\") }")
    }
    "instrument concrete trait construction" in {
      offsetsMatch("trait Foo { val foo = @\"foo\" }")
      offsetsMatch("trait Foo { def foo = @\"foo\" }")
    }
    "not instrument abstract trait construction" in {
      offsetsMatch("trait Foo { val foo: String }")
      offsetsMatch("trait Foo { def foo: String }")
    }
    "not instrument var's in traits" in {
      offsetsMatch("trait Foo { var foo: String }")
    }
    "not instrument abstract Type's" in {
      offsetsMatch("trait Foo { type T; val myThing: T }")
    }
  }

  "Class instrumentation" should {
    "basic fixed declarations" in {
      classOffsetsMatch("val z = @12;")
      classOffsetsMatch("var z = @12;")
      classOffsetsMatch("def z = @12;")
      classOffsetsMatch("def z { @12 }")
    }

    "unbound var declarations" in {
      // TODO! what about this?
      classOffsetsMatch("var xxx: String = _")
    }

    "plain calls" in {
      classOffsetsMatch("""@println("1")""")
      classOffsetsMatch("""@println("1"); @println("2");""")
    }
    "plain blocks" in {
      classOffsetsMatch("""{ @println("1"); @println("2") }""")
    }
    "inner classes" in {
      classOffsetsMatch("""class Inner @{ @println("1") }""")
    }
    "math operations" in {
      classOffsetsMatch("val eh = 12@/5.0");
    }
    "method bodys" in {
      classOffsetsMatch("def foo = @12")
      classOffsetsMatch("def foo = @hashCode")
      classOffsetsMatch("def foo = @System.currentTimeMillis")
      classOffsetsMatch("def foo = { @System.currentTimeMillis(); }");

    }
    "val bodys" in {
      classOffsetsMatch("val foo = @12")
      classOffsetsMatch("val foo = @hashCode")
      classOffsetsMatch("val foo = @System.currentTimeMillis")
    }
    "unit method bodys" in {
      classOffsetsMatch("def foo { @12; @hashCode; @System.currentTimeMillis }")
    }
    "nested blocks" in {
      classOffsetsMatch("def z = { @1; { @2; @3; { @4; @5; }; @6; }; @7 }")
    }
    "body content" in {
      offsetsMatch("""|class Foo@(x: Boolean) {
                            |  val y = @12
                            |  def z = @12
                            |  def defWithVal = {
                            |    val myVal = @y + System.getProperty("eh?") + methodWithInner
                            |    @myVal
                            |  }
                            |  def methodWithMap(s: String) =
                            |    @Some(s).map(@System.getProperty)
                            |  def methodWithInner = {
                            |    def innerMethod(i: Int) = @i * y
                            |    @0.to(z).map(@innerMethod).mkString
                            |  }
                            |}""".stripMargin)
    }
  }

  "Case class instrumentation" should {
    "overriden generated methods" in {
      offsetsMatch("case class Foo @{ override def hashCode = @999 }")
    }
  }

  "asInstanceOf" should {
    "normally" in {
      classOffsetsMatch("""val foo = @"x".asInstanceOf[scala.xml.Node]""")
      classOffsetsMatch("""def foo = @"x".asInstanceOf[scala.xml.Node]""")
      classOffsetsMatch("""def foo = { @println(x.toString); @"x".asInstanceOf[scala.xml.Node] }""")
    }
  }

  "Case class generated methods" should {
    "not instrument generated apply/unapply" in {
      offsetsMatch("object Foo { def x = @12 }\n\ncase class Foo@(y: Boolean) { def z = @11 }")
    }
  }

  "Types nested in a class" should {
    "instrument nested class" in {
      offsetsMatch("class Foo @{ class Bar @}") // TODO: how is the offset there?
      classOffsetsMatch("class Bar@(z: Int) {}")
    }
    "instrument nested case class" in {
      offsetsMatch("class Foo @{ case class Bar @}") // TODO: how is the offset there?
      classOffsetsMatch("case class Bar@(z: Int) {}")
    }
    "not instrument nested object" in {
      classOffsetsMatch("object Bar")
      classOffsetsMatch("object Bar {}")
    }
    "not instrument nested trait" in {
      classOffsetsMatch("trait Bar")
      classOffsetsMatch("trait Bar {}")
    }
    "not instrument nested case object" in {
      classOffsetsMatch("case object Bar")
      classOffsetsMatch("case object Bar {}")
    }
  }
}