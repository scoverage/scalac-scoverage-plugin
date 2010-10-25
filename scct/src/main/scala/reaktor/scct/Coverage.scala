package reaktor.scct

import report.HtmlReporter
import java.io.File
import java.util.concurrent.{Executors, TimeUnit}

object Coverage {
  var active = false
  val env = new Env
  env.reportHook match {
    case "system.property" => setupSystemPropertyHook
    case _ => setupShutdownHook
  }
  val data = readMetadata
  active = true

  @uncovered def invoked(id: String) {
    if (active) data.get(id).foreach { _.increment }
  }

  private def readMetadata = {
    try {
      val values = MetadataPickler.load(env.coverageFile)
      Map(values.map(x => (x.id, x)) :_*)
    } catch {
      case e => {
        System.err.println("Fail: "+e)
        e.printStackTrace
        throw e
      }
    }
  }

  def report = {
    HtmlReporter.report(data.values.toList, env)
  }

  private def setupShutdownHook {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run = {
        Coverage.active = false
        println("scct: Generating coverage report.")
        report
      }
    })
  }
  private def setupSystemPropertyHook {
    val prop = "scct.%s.fire.report".format(env.projectId)
    new Thread {
      override def run = {
        while (System.getProperty(prop, "") != "true") Thread.sleep(200)
        Coverage.active = false
        println("scct: Generating coverage report.")
        report
        System.setProperty(prop, "done")
      }
    }.start
  }
}

object ClassTypes {
  @serializable sealed abstract class ClassType
  @serializable case object Class extends ClassType
  @serializable case object Trait extends ClassType
  @serializable case object Object extends ClassType
  @serializable case object Package extends ClassType
  @serializable case object Root extends ClassType
}

@serializable case class Name(sourceFile: String, classType: ClassTypes.ClassType, packageName: String, className: String) extends Ordered[Name] {
  def compare(other: Name) = {
    lazy val classNameDiff = className.compareTo(other.className)
    lazy val classTypeDiff = classType.toString.compareTo(other.classType.toString)
    if (classNameDiff != 0) classNameDiff else classTypeDiff
  }
  override def toString = packageName+"/"+className+":"+sourceFile
}
@serializable case class CoveredBlock(id: String, name: Name, offset: Int, placeHolder: Boolean) {
  def this(id: String, name: Name, offset: Int)  = this(id, name, offset, false)
  var count = 0
  @uncovered def increment = { count = count + 1; this }
}

class uncovered extends StaticAnnotation

class Env {
  val projectId = System.getProperty("scct.project.name", "default")
  val reportHook = System.getProperty("scct.report.hook", "shutdown")
  val reportDir = new File(System.getProperty("scct.report.dir", "."))
  /** Where the source files actually start from, so e.g. $PROJECTHOME/src/main/scala/ */
  val sourceDir = new File(System.getProperty("scct.source.dir", "."))
  /** Where the source files are referenced from by the compiler, probably the root of the file system */
  val sourceBaseDir = new File(System.getProperty("scct.source.base.dir", ""))
  def coverageFile = sysOption("scct.coverage.file").map(new File(_)).getOrElse(new File(getClass.getResource("/coverage.data").toURI))

  private def sysOption(s: String) = {
    val value = System.getProperty(s)
    if (value == null) None else Some(value)
  }
}