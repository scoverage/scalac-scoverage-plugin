package scoverage

import org.scalatest.{FreeSpec, Matchers}

class LocationTest extends FreeSpec with Matchers {

  "location function" - {
    "should correctly process top level types" - {
      "for classes" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.test\nclass Sammy")
        val loc = compiler.locations.result.find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.test"
        loc.className shouldBe "Sammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for objects" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.test\nobject Bammy { def foo = 'boo } ")
        val loc = compiler.locations.result.find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.test"
        loc.className shouldBe "Bammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for traits" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.test\ntrait Gammy { def goo = 'hoo } ")
        val loc = compiler.locations.result.find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.test"
        loc.className shouldBe "Gammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
    "should correctly process methods" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.methodtest \n class Hammy { def foo = 'boo } ")
      val loc = compiler.locations.result.find(_._2.method == "foo").get._2
      loc.packageName shouldBe "com.methodtest"
      loc.className shouldBe "Hammy"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "should correctly process nested methods" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.methodtest \n class Hammy { def foo = { def goo = { getClass; 3 }; goo } } ")
      val loc = compiler.locations.result.find(_._2.method == "goo").get._2
      loc.packageName shouldBe "com.methodtest"
      loc.className shouldBe "Hammy"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")

    }
    "should process anon functions as inside the enclosing method" in {
      val compiler = ScoverageCompiler.locationCompiler
      compiler.compile("package com.methodtest \n class Jammy { def moo = { Option(\"bat\").map(_.length) } } ")
      val loc = compiler.locations.result.find(_._1 == "Function").get._2
      loc.packageName shouldBe "com.methodtest"
      loc.className shouldBe "Jammy"
      loc.method shouldBe "moo"
      loc.classType shouldBe ClassType.Class
      loc.sourcePath should endWith(".scala")
    }
    "should use outer package" - {
      "for nested classes" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { class Pammy } ")
        val loc = compiler.locations.result.find(_._2.className == "Pammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Pammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for nested objects" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { object Zammy } ")
        val loc = compiler.locations.result.find(_._2.className == "Zammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Zammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for nested traits" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { trait Mammy } ")
        val loc = compiler.locations.result.find(_._2.className == "Mammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Mammy"
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
        val loc = compiler.locations.result.find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.a.b"
        loc.className shouldBe "Kammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for objects" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.a \n " +
          "package b \n" +
          "object Kammy ")
        val loc = compiler.locations.result.find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.a.b"
        loc.className shouldBe "Kammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for traits" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.a \n " +
          "package b \n" +
          "trait Kammy ")
        val loc = compiler.locations.result.find(_._1 == "Template").get._2
        loc.packageName shouldBe "com.a.b"
        loc.className shouldBe "Kammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
    "should use <none> method name" - {
      "for class constructor body" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.b \n class Tammy { val name = 'sam } ")
        val loc = compiler.locations.result.find(_._1 == "ValDef").get._2
        loc.packageName shouldBe "com.b"
        loc.className shouldBe "Tammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
      "for object constructor body" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.b \n object Yammy { val name = 'sam } ")
        val loc = compiler.locations.result.find(_._1 == "ValDef").get._2
        loc.packageName shouldBe "com.b"
        loc.className shouldBe "Yammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
      "for trait constructor body" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.b \n trait Wammy { val name = 'sam } ")
        println()
        println(compiler.locations.result.mkString("\n"))
        val loc = compiler.locations.result.find(_._1 == "ValDef").get._2
        loc.packageName shouldBe "com.b"
        loc.className shouldBe "Wammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
  }
}

