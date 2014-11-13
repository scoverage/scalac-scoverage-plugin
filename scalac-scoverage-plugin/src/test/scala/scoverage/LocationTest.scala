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
    "for nested classes" - {
      "should use outer package" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { class Pammy } ")
        val loc = compiler.locations.result.find(_._2.className == "Pammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Pammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Class
        loc.sourcePath should endWith(".scala")
      }
    }
    "for nested objects" - {
      "should use outer package" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { object Zammy } ")
        val loc = compiler.locations.result.find(_._2.className == "Zammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Zammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Object
        loc.sourcePath should endWith(".scala")
      }
    }
    "for nested traits" - {
      "should use outer package" in {
        val compiler = ScoverageCompiler.locationCompiler
        compiler.compile("package com.methodtest \n class Jammy { trait Mammy } ")
        println()
        println(compiler.locations.result.mkString("\n"))
        val loc = compiler.locations.result.find(_._2.className == "Mammy").get._2
        loc.packageName shouldBe "com.methodtest"
        loc.className shouldBe "Mammy"
        loc.method shouldBe "<none>"
        loc.classType shouldBe ClassType.Trait
        loc.sourcePath should endWith(".scala")
      }
    }
  }
}

