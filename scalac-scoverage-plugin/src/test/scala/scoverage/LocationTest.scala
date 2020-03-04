package scoverage

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class LocationTest extends AnyFreeSpec with Matchers {

  "location function" - {
    "should correctly process top level types" - {
      "for classes" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.test\nclass Sammy")
        val loc = compiler.locations.result().find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.test"
        loc.className shouldBe "Sammy"
        loc.fullClassName shouldBe "com.test.Sammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for objects" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.test\nobject Bammy { def foo = Symbol(\"boo\") } ")
        val loc = compiler.locations.result().find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.test"
        loc.className shouldBe "Bammy"
        loc.fullClassName shouldBe "com.test.Bammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for traits" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.test\ntrait Gammy { def goo = Symbol(\"hoo\") } ")
        val loc = compiler.locations.result().find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.test"
        loc.className shouldBe "Gammy"
        loc.fullClassName shouldBe "com.test.Gammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
    "should correctly process methods" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.methodtest \n class Hammy { def foo = Symbol(\"boo\") } ")
      val loc = compiler.locations.result().find(_._2.method == "foo").get._2
      loc.packageName shouldBe "com.methodtest"
      loc.className shouldBe "Hammy"
      loc.fullClassName shouldBe "com.methodtest.Hammy"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "should correctly process nested methods" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.methodtest \n class Hammy { def foo = { def goo = { getClass; 3 }; goo } } ")
      val loc = compiler.locations.result().find(_._2.method == "goo").get._2
      loc.packageName shouldBe "com.methodtest"
      loc.className shouldBe "Hammy"
      loc.fullClassName shouldBe "com.methodtest.Hammy"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "should process anon functions as inside the enclosing method" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.methodtest \n class Jammy { def moo = { Option(\"bat\").map(_.length) } } ")
      val loc = compiler.locations.result().find(_._1 == "Function").get._2
      loc.packageName shouldBe "com.methodtest"
      loc.className shouldBe "Jammy"
      loc.fullClassName shouldBe "com.methodtest.Jammy"
      loc.method shouldBe "moo"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "should use outer package" - {
      "for nested classes" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { class Pammy } ")
        val loc = compiler.locations.result().find(_._2.className == "Pammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Pammy"
        loc.fullClassName shouldBe "com.methodtest.Jammy.Pammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for nested objects" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { object Zammy } ")
        val loc = compiler.locations.result().find(_._2.className == "Zammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Zammy"
        loc.fullClassName shouldBe "com.methodtest.Jammy.Zammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for nested traits" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { trait Mammy } ")
        val loc = compiler.locations.result().find(_._2.className == "Mammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Mammy"
        loc.fullClassName shouldBe "com.methodtest.Jammy.Mammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
    "should support nested packages" - {
      "for classes" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.a \n " +
          "package b \n" +
          "class Kammy ")
        val loc = compiler.locations.result().find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.a.b"
        loc.className shouldBe "Kammy"
        loc.fullClassName shouldBe "com.a.b.Kammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for objects" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.a \n " +
          "package b \n" +
          "object Kammy ")
        val loc = compiler.locations.result().find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.a.b"
        loc.className shouldBe "Kammy"
        loc.fullClassName shouldBe "com.a.b.Kammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for traits" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.a \n " +
          "package b \n" +
          "trait Kammy ")
        val loc = compiler.locations.result().find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.a.b"
        loc.className shouldBe "Kammy"
        loc.fullClassName shouldBe "com.a.b.Kammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
    "should use <none> method name" - {
      "for class constructor body" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.b \n class Tammy { val name = Symbol(\"sam\") } ")
        val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
        loc.packageName shouldBe "com.b"
        loc.className shouldBe "Tammy"
        loc.fullClassName shouldBe "com.b.Tammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for object constructor body" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.b \n object Yammy { val name = Symbol(\"sam\") } ")
        val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
        loc.packageName shouldBe "com.b"
        loc.className shouldBe "Yammy"
        loc.fullClassName shouldBe "com.b.Yammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for trait constructor body" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.b \n trait Wammy { val name = Symbol(\"sam\") } ")
        val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
        loc.packageName shouldBe "com.b"
        loc.className shouldBe "Wammy"
        loc.fullClassName shouldBe "com.b.Wammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
    "anon class should report enclosing class" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler
        .compile(
          "package com.a; object A { def foo(b : B) : Unit = b.invoke }; trait B { def invoke : Unit }; class C { A.foo(new B { def invoke = () }) }")
      val loc = compiler.locations.result().filter(_._1 == "Template").last._2
      loc.packageName shouldBe "com.a"
      loc.className shouldBe "C"
      loc.fullClassName shouldBe "com.a.C"
      loc.method shouldBe "<none>"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "anon class implemented method should report enclosing method" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile(
        "package com.a; object A { def foo(b : B) : Unit = b.invoke }; trait B { def invoke : Unit }; class C { A.foo(new B { def invoke = () }) }")
      val loc = compiler.locations.result().filter(_._1 == "DefDef").last._2
      loc.packageName shouldBe "com.a"
      loc.className shouldBe "C"
      loc.fullClassName shouldBe "com.a.C"
      loc.method shouldBe "invoke"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "doubly nested classes should report correct fullClassName" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.a \n object Foo { object Boo { object Moo { val name = Symbol(\"sam\") } } }")
      val loc = compiler.locations.result().find(_._1 == "ValDef").get._2
      loc.packageName shouldBe "com.a"
      loc.className shouldBe "Moo"
      loc.fullClassName shouldBe "com.a.Foo.Boo.Moo"
      loc.method shouldBe "<none>"
      loc.classType shouldBe ClassType.Object
      loc.sourcePath should endWith(".scala")
    }
  }
}
