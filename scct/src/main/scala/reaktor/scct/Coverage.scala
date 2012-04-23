package reaktor.scct

import java.io.File
import report.{CoberturaReporter, HtmlReporter}

object Coverage {
  var state = State.New
  var env: Env = _
  var data: Map[String, CoveredBlock] = _

  @uncovered def tryInit() {
    val oldState = state
    state = State.Starting
    if (!Env.isSbt || Env.sysOption("scct.project.name").isDefined) {
      init()
      state = State.Active
    } else {
      state = oldState
    }
  }

  def init() {
    env = new Env
    data = readMetadata
    env.reportHook match {
      case "system.property" => setupSystemPropertyHook
      case _ => setupShutdownHook
    }
  }

  @uncovered def invoked(id: String) {
    if (state == State.Active) {
      data.get(id).foreach { _.increment }
    } else if (state == State.New) {
      tryInit()
      if (state == State.Active) data.get(id).foreach { _.increment }
    }
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
    val dataList = data.values.toList
    HtmlReporter.report(dataList, env)
    CoberturaReporter.report(dataList, env)
  }

  private def setupShutdownHook {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run = {
        Coverage.state = State.Done
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
        Coverage.state = State.Done
        println("scct: Generating coverage report.")
        report
        System.setProperty(prop, "done")
      }
    }.start
  }
}

object ClassTypes {
  sealed abstract class ClassType extends Serializable
  case object Class extends ClassType
  case object Trait extends ClassType
  case object Object extends ClassType
  case object Package extends ClassType
  case object Root extends ClassType
}

case class Name(sourceFile: String, classType: ClassTypes.ClassType, packageName: String, className: String) extends Ordered[Name] {
  def compare(other: Name) = {
    lazy val classNameDiff = className.compareTo(other.className)
    lazy val classTypeDiff = classType.toString.compareTo(other.classType.toString)
    if (classNameDiff != 0) classNameDiff else classTypeDiff
  }
  override def toString = packageName+"/"+className+":"+sourceFile
}
case class CoveredBlock(id: String, name: Name, offset: Int, placeHolder: Boolean) {
  def this(id: String, name: Name, offset: Int)  = this(id, name, offset, false)
  var count = 0
  @uncovered def increment = { count = count + 1; this }
}

class uncovered extends scala.annotation.StaticAnnotation

class Env {
  val projectId = System.getProperty("scct.project.name", "default")
  val reportHook = System.getProperty("scct.report.hook", "shutdown")
  val reportDir = new File(System.getProperty("scct.report.dir", "."))
  /** Where the source files actually start from, so e.g. $PROJECTHOME/src/main/scala/ */
  val sourceDir = new File(System.getProperty("scct.source.dir", "."))
  def coverageFile = Env.sysOption("scct.coverage.file").map(new File(_)).getOrElse(new File(getClass.getResource("/coverage.data").toURI))
}

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

@uncovered object State extends Enumeration {
  val New, Starting, Active, Done = Value
}