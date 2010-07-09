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
    "handle package objects" in {
      packagesOf("package foo; package object bar { class X {} }") mustEqual List("foo.bar")
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
      classesOf("class X { def method { class Y } }") mustEqual List("X", "X.Y")      
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
    "handle classes in package objects" in {
      // TODO: placeholders of package objects currently have an empty class name ... that's probably ok?
      classesOf("package object foo { class X }") mustEqual List("", "X")
      classesOf("package object foo { def method { class X } }") mustEqual List("", "X")
    }
  }

  "type assignment" should {
    "handle all types" in {
      typesOf("class X") mustEqual List(ClassTypes.Class)
      typesOf("trait X") mustEqual List(ClassTypes.Trait)
      typesOf("object X") mustEqual List(ClassTypes.Object)
      typesOf("package object X") mustEqual List(ClassTypes.Package)
    }
  }

  private def packagesOf(s: String) = compile(s).map(_.name.packageName).distinct
  private def classesOf(s: String) = compile(s).map(_.name.className).distinct
  private def typesOf(s: String) = compile(s).map(_.name.classType).distinct
}