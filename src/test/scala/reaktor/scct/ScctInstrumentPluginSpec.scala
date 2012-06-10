package reaktor.scct

import org.specs.Specification
import org.specs.mock.Mockito
import tools.nsc.Global

class ScctInstrumentPluginSpec extends Specification with Mockito {
  val sut = new ScctInstrumentPlugin(smartMock[Global])
  "Plugin" should {
    "provide metadata" in {
      sut.name mustEqual "scct"
      sut.description mustEqual "Scala code coverage instrumentation plugin."
    }
    "run after refchecks" in {
      sut.runsAfter mustEqual List("refchecks")
    }
    "only contain the transformer component" in {
      val components = sut.components
      components.size mustEqual 1
      components(0) must haveClass[ScctTransformComponent]
    }
  }
}