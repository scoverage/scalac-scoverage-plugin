package reaktor.scct

import java.io.File

object Env {
  def sysOption(s: String) = {
    val value = System.getProperty(s)
    if (value == null) None else Some(value)
  }
  lazy val isSbt = {
    matchSbtClassLoader(getClass.getClassLoader)
  }
  def matchSbtClassLoader(cl: ClassLoader): Boolean = {
    if (cl == null) {
      false
    } else if (cl.getClass.getName == "xsbt.DualLoader") {
      true
    } else {
      matchSbtClassLoader(cl.getParent)
    }
  }
}

class Env {
  val projectId = System.getProperty("scct.project.name", "default")
  val baseDir = new File(System.getProperty("scct.basedir", System.getProperty("user.dir", ".")))
  val reportHook = System.getProperty("scct.report.hook", "shutdown")
  val reportDir = new File(System.getProperty("scct.report.dir", "."))

  /** Where the source files actually start from, so e.g. $PROJECTHOME/src/main/scala/ */
  val sourceDir = new File(System.getProperty("scct.source.dir", "."))

  def coverageFile = Env.sysOption("scct.coverage.file").map(new File(_)).getOrElse(new File(getClass.getResource("/coverage.data").toURI))
}

