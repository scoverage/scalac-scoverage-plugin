package reaktor.scct

import org.specs2.mutable._

class EnvSpec extends Specification {
  "Env" should {
    "provide Option[String]'s" in {
      val propName = "scct.test."+System.currentTimeMillis
      try {
        Env.sysOption(propName) mustEqual None
        System.setProperty(propName, "1")
        Env.sysOption(propName) mustEqual Some("1")
      } finally {
        System.clearProperty(propName)
      }
    }
    "read from prop file" in {
      val env = new Env {
        override val props = Env.envProps("/scct-example.properties")
      }
      env.projectId mustEqual "Foobar"
      env.reportHook mustEqual "shutdown"
    }
  }
}
