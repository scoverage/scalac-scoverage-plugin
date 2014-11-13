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
        println()
        println(compiler.locations.result.mkString("\n"))
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
  }
}

