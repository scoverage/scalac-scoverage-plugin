package reaktor.scct.report

import org.specs.Specification
import java.io.File

class SourceLoaderSpec extends Specification {

  val sut = new SourceLoader()

  "Handle windows line feeds" in {
    val lines = sut.linesFor(path("WindowsCRLF.scala"))
    lines(0) mustEqual "class Foo { def x = 123\r\n"
  }

  "Handle \\n line feeds" in {
    val lines = sut.linesFor(path("MacOSX.scala"))
    lines(0) mustEqual "class MacFoo { def x = 123\n"
  }

  private def path(f: String) = new File(new File(".", "src/test/resources"), f).getCanonicalPath

}