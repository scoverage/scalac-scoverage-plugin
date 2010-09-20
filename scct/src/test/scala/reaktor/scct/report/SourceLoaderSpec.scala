package reaktor.scct.report

import org.specs.Specification
import reaktor.scct.Env
import java.io.File

class SourceLoaderSpec extends Specification {
  val env = new Env {
    override val sourceBaseDir = new File(".", "src/test/resources")
  }
  val sut = new SourceLoader(env)

  "Handle windows line feeds" in {
    val lines = sut.linesFor("WindowsCRLF.scala")
    lines(0) mustEqual "class Foo { def x = 123\r\n"
  }

  "Handle \\n line feeds" in {
    val lines = sut.linesFor("MacOSX.scala")
    lines(0) mustEqual "class MacFoo { def x = 123\n"
  }
}