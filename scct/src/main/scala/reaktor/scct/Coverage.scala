package reaktor.scct

import report.HtmlReporter
import java.io.File

object Coverage {
  var active = false
  setupShutdownHook
  val data = readMetadata
  active = true

  @uncovered def invoked(id: String) {
    if (active) data.get(id).foreach { _.increment }
  }

  private def readMetadata = {
    try {
      val values = MetadataPickler.load
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
    HtmlReporter.report(data.values.toList, new File(System.getProperty("scct.report.dir", ".")))
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
}

object ClassTypes {
  @serializable sealed abstract class ClassType
  @serializable case object Class extends ClassType
  @serializable case object Trait extends ClassType
  @serializable case object Object extends ClassType
  @serializable case object Root extends ClassType
}

@serializable case class Name(sourceFile: String, classType: ClassTypes.ClassType, packageName: String, className: String) extends Ordered[Name] {
  def compare(other: Name) = {
    lazy val classNameDiff = className.compareTo(other.className)
    lazy val classTypeDiff = classType.toString.compareTo(other.classType.toString)
    if (classNameDiff != 0) classNameDiff else classTypeDiff
  }
}
@serializable case class CoveredBlock(id: String, name: Name, offset: Int, placeHolder: Boolean) {
  def this(id: String, name: Name, offset: Int)  = this(id, name, offset, false)
  var count = 0
  @uncovered def increment = { count = count + 1; this }
}

class uncovered extends StaticAnnotation
