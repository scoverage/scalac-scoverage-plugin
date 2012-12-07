package reaktor.scct

import org.specs2.mutable._
import org.specs2.mock._
import tools.nsc.Global
import java.io.File

class ScctInstrumentPluginSpec extends Specification with Mockito {
  val sut = new ScctInstrumentPlugin(smartMock[Global])
  "Plugin" should {
    "provide metadata" in {
      sut.name mustEqual "scct"
      sut.description mustEqual "Scala code coverage instrumentation plugin."
    }
    "only contain the transformer component" in {
      val components = sut.components
      components.size mustEqual 1
      components(0) must haveClass[ScctTransformComponent]
    }
    "run after typer and before patmat" in {
      val c = sut.components(0)
      c.runsAfter mustEqual List("typer")
      c.runsBefore mustEqual List("patmat")
    }
  }

  "Plugin options" should {
    "have defaults" in {
      sut.options.projectId must not be empty
      sut.options.baseDir.getName must not be empty
    }
    "be settable" in {
      sut.processOptions(List("basedir:/base/dir", "projectId:myProject"), s => ())
      sut.options.projectId mustEqual "myProject"
      sut.options.baseDir.getAbsolutePath mustEqual "/base/dir"
    }
    "report error" in {
      var err = ""
      sut.processOptions(List("wrong:option"), s => { err = s })
      err mustEqual "Unknown option: wrong:option"
    }
  }
}