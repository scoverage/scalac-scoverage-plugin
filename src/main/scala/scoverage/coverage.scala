package scoverage

import java.io.File
import scala.collection.mutable

/**
 * @author Stephen Samuel */
case class Coverage()
  extends CoverageMetrics
  with MethodBuilders
  with java.io.Serializable
  with ClassBuilders
  with PackageBuilders
  with FileBuilders {

  private val statementsById = mutable.Map[Int, MeasuredStatement]()
  override def statements = statementsById.values
  def add(stmt: MeasuredStatement): Unit = statementsById.put(stmt.id, stmt)

  def avgClassesPerPackage = classCount / packageCount.toDouble
  def avgClassesPerPackageFormatted: String = "%.2f".format(avgClassesPerPackage)

  def avgMethodsPerClass = methodCount / classCount.toDouble
  def avgMethodsPerClassFormatted: String = "%.2f".format(avgMethodsPerClass)

  def loc = files.map(_.loc).sum
  def linesPerFile = loc / fileCount.toDouble
  def linesPerFileFormatted: String = "%.2f".format(linesPerFile)

  // returns the classes by least coverage
  def risks(limit: Int) = classes.toSeq.sortBy(_.statementCount).reverse.sortBy(_.statementCoverage).take(limit)

  def apply(ids: Iterable[Int]): Unit = ids foreach invoked
  def invoked(id: Int): Unit = statementsById.get(id).foreach(_.invoked())
}

trait MethodBuilders {
  def statements: Iterable[MeasuredStatement]
  def methods: Seq[MeasuredMethod] = {
    statements.groupBy(stmt => stmt.location._package + "/" + stmt.location._class + "/" + stmt.location.method)
      .map(arg => MeasuredMethod(arg._1, arg._2))
      .toSeq
  }
  def methodCount = methods.size
}

trait PackageBuilders {
  def statements: Iterable[MeasuredStatement]
  def packageCount = packages.size
  def packages: Seq[MeasuredPackage] = {
    statements.groupBy(_.location._package).map(arg => MeasuredPackage(arg._1, arg._2)).toSeq.sortBy(_.name)
  }
}

trait ClassBuilders {
  def statements: Iterable[MeasuredStatement]
  def classes = statements.groupBy(_.location._class).map(arg => MeasuredClass(arg._1, arg._2))
  def classCount: Int = classes.size
}

trait FileBuilders {
  def statements: Iterable[MeasuredStatement]
  def files: Iterable[MeasuredFile] = statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
  def fileCount: Int = files.size
}

case class MeasuredMethod(name: String, statements: Iterable[MeasuredStatement]) extends CoverageMetrics

case class MeasuredClass(name: String, statements: Iterable[MeasuredStatement])
  extends CoverageMetrics with MethodBuilders {
  def source: String = statements.head.source
  def simpleName = name.split('.').last
  def loc = statements.map(_.line).max
}

case class MeasuredPackage(name: String, statements: Iterable[MeasuredStatement])
  extends CoverageMetrics with ClassCoverage with ClassBuilders with FileBuilders {
}

case class MeasuredFile(source: String, statements: Iterable[MeasuredStatement])
  extends CoverageMetrics with ClassCoverage with ClassBuilders {
  def filename = new File(source).getName
  def loc = statements.map(_.line).max
}

case class MeasuredStatement(source: String,
                             location: Location,
                             id: Int,
                             start: Int,
                             end: Int,
                             line: Int,
                             desc: String,
                             symbolName: String,
                             treeName: String,
                             branch: Boolean,
                             var count: Int = 0) extends java.io.Serializable {
  def invoked(): Unit = count = count + 1
  def isInvoked = count > 0
}

case class Location(_package: String,
                    _class: String,
                    classType: ClassType,
                    method: String)
  extends java.io.Serializable {
  val fqn = (_package + ".").replace("<empty>.", "") + _class
}

case class ClassRef(name: String) {
  lazy val simpleName = name.split(".").last
  lazy val getPackage = name.split(".").dropRight(1).mkString(".")
}
object ClassRef {
  def fromFilepath(path: String) = ClassRef(path.replace('/', '.'))
}

trait CoverageMetrics {
  def statements: Iterable[MeasuredStatement]
  def statementCount: Int = statements.size
  def invokedStatements: Iterable[MeasuredStatement] = statements.filter(_.count > 0)
  def invokedStatementCount = invokedStatements.size
  def statementCoverage: Double = if (statementCount == 0) 1 else invokedStatementCount / statementCount.toDouble
  def statementCoveragePercent = statementCoverage * 100
  def statementCoverageFormatted: String = "%.2f".format(statementCoveragePercent)
  def branches: Iterable[MeasuredStatement] = statements.filter(_.branch)
  def branchCount: Int = branches.size
  def branchCoveragePercent = branchCoverage * 100
  def invokedBranches: Iterable[MeasuredStatement] = branches.filter(_.count > 0)
  def invokedBranchesCount = invokedBranches.size
  def branchCoverage: Double = if (branchCount == 0) 1 else invokedBranchesCount / branchCount.toDouble
  def branchCoverageFormatted: String = "%.2f".format(branchCoveragePercent)
}

trait ClassCoverage {
  this: ClassBuilders =>
  val statements: Iterable[MeasuredStatement]
  def invokedClasses: Int = classes.count(_.statements.count(_.count > 0) > 0)
  def classCoverage: Double = invokedClasses / classes.size.toDouble
}
