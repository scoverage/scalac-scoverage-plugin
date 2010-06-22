package reaktor.scct

import java.net.URLClassLoader

class DebugInstrumentationSpec extends InstrumentationSpec {

  "debug" in {
    //println("CP:\n"+System.getProperty("java.class.path").split(":").mkString("\n"))
    //debugCls(getClass.getClassLoader)
    offsetsMatch("class Foo @{ class Bar @}")
    //defOffsetsMatch("var z = @0; while (z < 5) @z += 1")
  }

  private def debugCls(cl: ClassLoader) {
    if (cl == null) {
      println("CL: null")
    } else {
      cl match {
        case urlCl: URLClassLoader => println("CL:\n"+urlCl.getURLs.map(_.toString).mkString("\n"))
        case _ => println("Odd classloader: "+cl.getClass)
      }
      debugCls(cl.getParent)
    }
  }
}