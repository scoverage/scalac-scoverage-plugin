package scales

import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.SourceFile

/**
 * @author Stephen Samuel */
class Coverage
  extends CoverageMetrics with
  MethodBuilders with
  java.io.Serializable with
  ClassBuilders with
  PackageBuilders with
  Numerics {

  val statements = new ListBuffer[MeasuredStatement]

  def add(stmt: MeasuredStatement): Unit = statements.append(stmt)

  def avgClassesPerPackage = classCount / packageCount.toDouble
  def avgMethodsPerClass = methodCount / classCount.toDouble

  // returns the classes by least coverage
  def risks(limit: Int) = classes.toSeq.sortBy(_.statementCoverage).take(limit)

  def apply(ids: Iterable[Int]): Unit = ids foreach invoked
  def invoked(id: Int): Unit = statements.find(_.id == id).foreach(_.invoked())
}

trait MethodBuilders {
  val statements: Iterable[MeasuredStatement]
  def methodCount = methods.size
  def methods: Seq[MeasuredMethod] = {
    statements.groupBy(_.location.method).map(arg => MeasuredMethod(arg._1, arg._2)).toSeq
  }
}

trait PackageBuilders {
  val statements: Iterable[MeasuredStatement]
  def packageCount = packages.size
  def packages: Seq[MeasuredPackage] = {
    statements.groupBy(_.location._package).map(arg => MeasuredPackage(arg._1, arg._2)).toSeq.sortBy(_.name)
  }
}

trait ClassBuilders {
  val statements: Iterable[MeasuredStatement]
  def classes = statements.groupBy(_.location._class).map(arg => MeasuredClass(arg._1, arg._2))
  def classCount: Int = classes.size
}

case class MeasuredMethod(name: String, statements: Iterable[MeasuredStatement]) extends CoverageMetrics

case class MeasuredClass(name: String, statements: Iterable[MeasuredStatement])
  extends CoverageMetrics with MethodBuilders with Numerics

case class MeasuredPackage(name: String, statements: Iterable[MeasuredStatement])
  extends CoverageMetrics with ClassCoverage with ClassBuilders {
  def files = Nil //statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
}

case class MeasuredFile(source: SourceFile, statements: Iterable[MeasuredStatement])
  extends CoverageMetrics with ClassCoverage with ClassBuilders {
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
                             branch: Boolean,
                             var count: Int = 0) extends java.io.Serializable {
  def invoked(): Unit = count = count + 1
}

trait Numerics {
  val statements: Iterable[MeasuredStatement]
  def loc = statements.map(stmt => stmt.location.fqn + ":" + stmt.line).toSet.size
}

case class Location(_package: String, _class: String, classType: ClassType, method: String)
  extends java.io.Serializable {
  val fqn = (_package + ".").replace("<empty>.", "") + _class
}

trait CoverageMetrics {
  val statements: Iterable[MeasuredStatement]
  def statementCount: Int = statements.count(!_.branch)
  def invokedStatements: Iterable[MeasuredStatement] = statements.filter(_.count > 0)
  def invokedStatementCount = invokedStatements.size
  def statementCoverage: Double = if (invokedStatementCount == 0) 0 else invokedStatementCount / statementCount.toDouble
  def statementCoverageFormatted: String = "%.2f".format(statementCoverage * 100)
  def branches: Iterable[MeasuredStatement] = statements.filter(_.branch)
  def branchCount: Int = branches.size
  def invokedBranches: Iterable[MeasuredStatement] = branches.filter(_.count > 0)
  def invokedBranchesCount = invokedBranches.size
  def branchCoverage: Double = if (branchCount == 0) 0 else invokedBranchesCount / branchCount.toDouble
  def branchCoverageFormatted: String = "%.2f".format(branchCoverage * 100)
}

trait ClassCoverage {
  this: ClassBuilders =>
  val statements: Iterable[MeasuredStatement]
  def invokedClasses: Int = classes.count(_.statements.count(_.count > 0) > 0)
  def classCoverage: Double = invokedClasses / classes.size.toDouble
}
