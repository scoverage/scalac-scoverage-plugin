package reaktor.scct

class NameParsingInstrumentationSpec extends InstrumentationSpec {
  "package name assignment" should {
    "treat empty package as <root>" in {
      packagesOf("class X {}") mustEqual List("<root>")
      packagesOf("trait X {}") mustEqual List("<root>")
    }
    "handle nested packages" in {
      packagesOf("package foo { package bar { package baz { class X {} } } }") mustEqual List("foo.bar.baz")
    }
    "handle declared packages" in {
      packagesOf("package foo.bar; class X {}") mustEqual List("foo.bar")
    }
  }

  "class name assignment" should {
    "find basic class names" in {
      classesOf("class X") mustEqual List("X")
      classesOf("trait X") mustEqual List("X")
      classesOf("object X") mustEqual List("X")
      classesOf("case class X") mustEqual List("X")
    }
    "handle nested classes" in {
      classesOf("class X { class Y }") mustEqual List("X", "X.Y")
    }
    "handle classes nested inside methods" in {
      classesOf("class X { def method { class Y; 1 } }") mustEqual List("X.Y", "X")
    }
    "skip anonymous classes" in {
      classesOf("class X { def go = 1 }; class Y { new X { override def go = 2 } }") mustEqual List("X", "Y")
    }
    "skip nested anonymous classes" in {
      classesOf("class First; class Second; class Holder { new First { new Second {} } }") mustEqual List("First", "Second", "Holder")
    }
    "handle nested classes in anonymous classes" in {
      classesOf("class AnonX; class Holder { new AnonX { class Inner {} } }") mustEqual List("AnonX", "Holder", "Holder.Inner")
    }
  }

  "type assignment" should {
    "handle all types" in {
      typesOf("class X") mustEqual List(ClassTypes.Class)
      typesOf("trait X") mustEqual List(ClassTypes.Trait)
      typesOf("object X") mustEqual List(ClassTypes.Object)
    }
  }

  private def packagesOf(s: String) = compile(s).map(_.name.packageName).removeDuplicates
  private def classesOf(s: String) = compile(s).map(_.name.className).removeDuplicates
  private def typesOf(s: String) = compile(s).map(_.name.classType).removeDuplicates
}