package scales

import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.SourceFile

/**
 * @author Stephen Samuel */
class Coverage extends StatementCoverage with java.io.Serializable {

  val statements = new ListBuffer[MeasuredStatement]

  def add(stmt: MeasuredStatement): Unit = statements.append(stmt)

  def loc = 0 //sources.map(src => new String(src.content).replaceAll("^\\s.*$", "").split("\n").length).sum

  def ncloc = 0 //{
  //    sources
  //      .map(src => new String(src.content)
  //      .replaceAll("/\\*.*?\\*/", "")
  //      .replace("//.*$", "")
  //      .split("\n")
  //      .count(_ == '\n')).sum
  //  }

  def packageCount = packages.size
  def packages: Seq[MeasuredPackage] = {
    statements.groupBy(_.location._package).map(arg => MeasuredPackage(arg._1, arg._2)).toSeq.sortBy(_.name)
  }

  def classCount = classes.size
  def classes: Seq[MeasuredClass] = {
    statements.groupBy(_.location.fqn).map(arg => MeasuredClass(arg._1, arg._2)).toSeq.sortBy(_.name)
  }

  def methodCount = methods.size
  def methods: Set[String] = statements.flatMap(_.location.method).toSet

  def avgClassesPerPackage = classCount / packageCount.toDouble
  def avgMethodsPerClass = methodCount / classCount.toDouble

  // returns the classes by least coverage
  def risks(limit: Int) = classes.toSeq.sortBy(_.statementCoverage).take(limit)

  def apply(ids: Iterable[Int]): Unit = ids foreach invoked
  def invoked(id: Int): Unit = statements.find(_.id == id).foreach(_.invoked())
}

case class MeasuredClass(name: String, statements: Iterable[MeasuredStatement]) extends StatementCoverage

case class MeasuredPackage(name: String, statements: Iterable[MeasuredStatement])
  extends StatementCoverage with ClassCoverage {
  def files = Nil //statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
}

case class MeasuredFile(source: SourceFile, statements: Iterable[MeasuredStatement])
  extends StatementCoverage with ClassCoverage {
  def lineStatus(lineNumber: Int): LineStatus = {
    statements.filter(_.line == lineNumber) match {
      case i if i.isEmpty => NotInstrumented
      case i if i.size > 0 && i.exists(_.count == 0) => MissingCoverage
      case _ => Covered
    }
  }
  def packages: Iterable[MeasuredPackage] = statements
    .groupBy(_.location._package)
    .map(arg => MeasuredPackage(arg._1, arg._2))
}

case class MeasuredStatement(location: Location,
                             id: Int,
                             start: Int,
                             line: Int,
                             desc: String,
                             var count: Int = 0) extends java.io.Serializable {
  def invoked(): Unit = count = count + 1
}

case class Location(_package: String, _class: String, method: Option[String]) extends java.io.Serializable {
  val fqn = (_package + ".").replace("<empty>.", "") + _class
}

trait StatementCoverage {
  val statements: Iterable[MeasuredStatement]
  def statementCoverage: Double = invokedStatements / statements.size.toDouble
  def statementCount: Int = statements.size
  def invokedStatements: Int = statements.count(_.count > 0)
}

trait ClassCoverage {
  val statements: Iterable[MeasuredStatement]
  def classes = statements.groupBy(_.location._class).map(arg => MeasuredClass(arg._1, arg._2))
  def classCount: Int = classes.size
  def invokedClasses: Int = classes.count(_.statements.count(_.count > 0) > 0)
  def classCoverage: Double = invokedClasses / classes.size.toDouble
}
