package reaktor.scct

import java.io.File
import java.util.Properties

object Env {
  def sysOption(s: String) = {
    Option(System.getProperty(s))
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

  def envProps(propertyFileResourceName: String) = {
    val props = new Properties(sysProps)
    Option(getClass.getResourceAsStream(propertyFileResourceName)).map(props.load)
    props
  }

  def sysProps = {
    val props = new Properties
    def put(s: String, d:String) = props.put(s, System.getProperty(s, d))
    put("scct.project.name", "default")
    put("scct.basedir", System.getProperty("user.dir", "."))
    put("scct.report.hook", "shutdown")
    put("scct.report.dir", ".")
    put("scct.source.dir", ".")
    props
  }
}

class Env {
  val props = Env.envProps("/scct.properties")
  def prop(x: String) = props.getProperty(x)

  lazy val projectId = prop("scct.project.name")
  lazy val baseDir = new File(prop("scct.basedir"))
  lazy val reportHook = prop("scct.report.hook")
  lazy val reportDir = new File(prop("scct.report.dir"))

  /** Where the source files actually start from, so e.g. $PROJECTHOME/src/main/scala/ */
  lazy val sourceDir = new File(prop("scct.source.dir"))

  def coverageFile = Env.sysOption("scct.coverage.file").map(new File(_)).getOrElse(new File(getClass.getResource("/coverage.data").toURI))
}

