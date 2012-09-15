package reaktor.scct

import report._

object Coverage {
  @uncovered private var state = State.New
  @uncovered private lazy val env: Env = new Env
  @uncovered private lazy val data: Map[String, CoveredBlock] = {
    state = State.Starting
    val metaData = readMetadata
    env.reportHook match {
      case "system.property" => setupSystemPropertyHook
      case _                 => setupShutdownHook
    }
    state = State.Active
    metaData
  }

  @uncovered def invoked(id: String) {
    data.get(id).foreach { _.increment }
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
    val projectData = new ProjectData(env, data.values.toList)
    val writer = new HtmlReportWriter(env.reportDir)
    new HtmlReporter(projectData, writer).report
    new CoberturaReporter(projectData, writer).report
    BinaryReporter.report(projectData, env.reportDir)
  }

  private def setupShutdownHook {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run = {
        Coverage.state = State.Done
        println("scct: [" + env.projectId + "] Generating coverage report.")
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
        println("scct: [" + env.projectId + "] Generating coverage report.")
        report
        System.setProperty(prop, "done")
      }
    }.start
  }
}

@uncovered object ClassTypes {
  @SerialVersionUID(1L) sealed abstract class ClassType extends Serializable
  @SerialVersionUID(1L) case object Class extends ClassType
  @SerialVersionUID(1L) case object Trait extends ClassType
  @SerialVersionUID(1L) case object Object extends ClassType
  @SerialVersionUID(1L) case object Package extends ClassType
  @SerialVersionUID(1L) case object Root extends ClassType
}

@SerialVersionUID(1L) case class Name(sourceFile: String, classType: ClassTypes.ClassType, packageName: String, className: String, projectName:String) extends Ordered[Name] {
  def compare(other: Name) = {
    lazy val classNameDiff = className.compareTo(other.className)
    lazy val classTypeDiff = classType.toString.compareTo(other.classType.toString)
    lazy val projectNameDiff = projectName.compareTo(other.projectName)
    if (classNameDiff != 0) classNameDiff else if (classTypeDiff != 0) classTypeDiff else projectNameDiff
  }
  override def toString = projectName+":"+packageName+"/"+className+":"+sourceFile
}

@SerialVersionUID(1L) case class CoveredBlock(id: String, name: Name, offset: Int, placeHolder: Boolean) {
  def this(id: String, name: Name, offset: Int)  = this(id, name, offset, false)
  var count = 0
  @uncovered def increment = { count = count + 1; this }
}

class uncovered extends scala.annotation.StaticAnnotation

@uncovered object State extends Enumeration {
  val New, Starting, Active, Done = Value
}