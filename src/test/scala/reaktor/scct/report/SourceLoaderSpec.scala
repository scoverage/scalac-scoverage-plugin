package reaktor.scct.report

import org.specs2.mutable._
import java.io.File

class SourceLoaderSpec extends Specification {

  val sut = new SourceLoader(new File("src/test/resources"))

  "Handle windows line feeds" in {
    val lines = sut.linesFor("WindowsCRLF.scala")
    lines(0) mustEqual "class Foo { def x = 123\r\n"
  }

  "Handle \\n line feeds" in {
    val lines = sut.linesFor("MacOSX.scala")
    lines(0) mustEqual "class MacFoo { def x = 123\n"
  }

}