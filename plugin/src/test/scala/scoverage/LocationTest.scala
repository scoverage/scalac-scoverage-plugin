package scoverage

import munit.FunSuite
import scoverage.domain.ClassType

class LocationTest extends FunSuite {

  test("top level for classes") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile("package com.test\nclass Sammy")
    val loc = compiler.locations.result().find(_._1 == "Template").get._2
    assertEquals(loc.packageName, "com.test")
    assertEquals(loc.className, "Sammy")
    assertEquals(loc.fullClassName, "com.test.Sammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("top level for objects") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.test\nobject Bammy { def foo = Symbol(\"boo\") } "
    )
    val loc = compiler.locations.result().find(_._1 == "Template").get._2
    assertEquals(loc.packageName, "com.test")
    assertEquals(loc.className, "Bammy")
    assertEquals(loc.fullClassName, "com.test.Bammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Object)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("top level for traits") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.test\ntrait Gammy { def goo = Symbol(\"hoo\") } "
    )
    val loc = compiler.locations.result().find(_._1 == "Template").get._2
    assertEquals(loc.packageName, "com.test")
    assertEquals(loc.className, "Gammy")
    assertEquals(loc.fullClassName, "com.test.Gammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Trait)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("should correctly process methods") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.methodtest \n class Hammy { def foo = Symbol(\"boo\") } "
    )
    val loc = compiler.locations.result().find(_._2.method == "foo").get._2
    assertEquals(loc.packageName, "com.methodtest")
    assertEquals(loc.className, "Hammy")
    assertEquals(loc.fullClassName, "com.methodtest.Hammy")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("should correctly process nested methods") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.methodtest \n class Hammy { def foo = { def goo = { getClass; 3 }; goo } } "
    )
    val loc = compiler.locations.result().find(_._2.method == "goo").get._2
    assertEquals(loc.packageName, "com.methodtest")
    assertEquals(loc.className, "Hammy")
    assertEquals(loc.fullClassName, "com.methodtest.Hammy")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("should process anon functions as inside the enclosing method") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.methodtest \n class Jammy { def moo = { Option(\"bat\").map(_.length) } } "
    )
    val loc = compiler.locations.result().find(_._1 == "Function").get._2
    assertEquals(loc.packageName, "com.methodtest")
    assertEquals(loc.className, "Jammy")
    assertEquals(loc.fullClassName, "com.methodtest.Jammy")
    assertEquals(loc.method, "moo")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("should use outer package for nested classes") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.methodtest \n class Jammy { class Pammy } "
    )
    val loc =
      compiler.locations.result().find(_._2.className == "Pammy").get._2
    assertEquals(loc.packageName, "com.methodtest")
    assertEquals(loc.className, "Pammy")
    assertEquals(loc.fullClassName, "com.methodtest.Jammy.Pammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("for nested objects") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.methodtest \n class Jammy { object Zammy } "
    )
    val loc =
      compiler.locations.result().find(_._2.className == "Zammy").get._2
    assertEquals(loc.packageName, "com.methodtest")
    assertEquals(loc.className, "Zammy")
    assertEquals(loc.fullClassName, "com.methodtest.Jammy.Zammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Object)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("for nested traits") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.methodtest \n class Jammy { trait Mammy } "
    )
    val loc =
      compiler.locations.result().find(_._2.className == "Mammy").get._2
    assertEquals(loc.packageName, "com.methodtest")
    assertEquals(loc.className, "Mammy")
    assertEquals(loc.fullClassName, "com.methodtest.Jammy.Mammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Trait)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("should support nested packages for classes") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.a \n " +
        "package b \n" +
        "class Kammy "
    )
    val loc = compiler.locations.result().find(_._1 == "Template").get._2
    assertEquals(loc.packageName, "com.a.b")
    assertEquals(loc.className, "Kammy")
    assertEquals(loc.fullClassName, "com.a.b.Kammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("for objects") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.a \n " +
        "package b \n" +
        "object Kammy "
    )
    val loc = compiler.locations.result().find(_._1 == "Template").get._2
    assertEquals(loc.packageName, "com.a.b")
    assertEquals(loc.className, "Kammy")
    assertEquals(loc.fullClassName, "com.a.b.Kammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Object)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("for traits") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.a \n " +
        "package b \n" +
        "trait Kammy "
    )
    val loc = compiler.locations.result().find(_._1 == "Template").get._2
    assertEquals(loc.packageName, "com.a.b")
    assertEquals(loc.className, "Kammy")
    assertEquals(loc.fullClassName, "com.a.b.Kammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Trait)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("should use <none> method name for class constructor body") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.b \n class Tammy { val name = Symbol(\"sam\") } "
    )
    val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
    assertEquals(loc.packageName, "com.b")
    assertEquals(loc.className, "Tammy")
    assertEquals(loc.fullClassName, "com.b.Tammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("for object constructor body") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.b \n object Yammy { val name = Symbol(\"sam\") } "
    )
    val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
    assertEquals(loc.packageName, "com.b")
    assertEquals(loc.className, "Yammy")
    assertEquals(loc.fullClassName, "com.b.Yammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Object)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("for trait constructor body") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.b \n trait Wammy { val name = Symbol(\"sam\") } "
    )
    val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
    assertEquals(loc.packageName, "com.b")
    assertEquals(loc.className, "Wammy")
    assertEquals(loc.fullClassName, "com.b.Wammy")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Trait)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("anon class should report enclosing class") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler
      .compile(
        "package com.a; object A { def foo(b : B) : Unit = b.invoke }; trait B { def invoke : Unit }; class C { A.foo(new B { def invoke = () }) }"
      )
    val loc = compiler.locations.result().filter(_._1 == "Template").last._2
    assertEquals(loc.packageName, "com.a")
    assertEquals(loc.className, "C")
    assertEquals(loc.fullClassName, "com.a.C")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("anon class implemented method should report enclosing method") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.a; object A { def foo(b : B) : Unit = b.invoke }; trait B { def invoke : Unit }; class C { A.foo(new B { def invoke = () }) }"
    )
    val loc = compiler.locations.result().filter(_._1 == "DefDef").last._2
    assertEquals(loc.packageName, "com.a")
    assertEquals(loc.className, "C")
    assertEquals(loc.fullClassName, "com.a.C")
    assertEquals(loc.method, "invoke")
    assertEquals(loc.classType, ClassType.Class)
    assert(loc.sourcePath.endsWith(".scala"))
  }
  test("doubly nested classes should report correct fullClassName") {
    val compiler = ScoverageCompiler.locationCompiler
    compiler.compile(
      "package com.a \n object Foo { object Boo { object Moo { val name = Symbol(\"sam\") } } }"
    )
    val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
    assertEquals(loc.packageName, "com.a")
    assertEquals(loc.className, "Moo")
    assertEquals(loc.fullClassName, "com.a.Foo.Boo.Moo")
    assertEquals(loc.method, "<none>")
    assertEquals(loc.classType, ClassType.Object)
    assert(loc.sourcePath.endsWith(".scala"))
  }
}
