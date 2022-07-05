package scoverage.domain

import scala.collection.mutable

/** @author Stephen Samuel
  */
case class Coverage()
    extends CoverageMetrics
    with MethodBuilders
    with java.io.Serializable
    with ClassBuilders
    with PackageBuilders
    with FileBuilders {

  private val statementsById = mutable.Map[Int, Statement]()
  override def statements = statementsById.values
  def add(stmt: Statement): Unit = statementsById.put(stmt.id, stmt)
  def remove(id: Int): Unit = statementsById.remove(id)

  private val ignoredStatementsById = mutable.Map[Int, Statement]()
  override def ignoredStatements = ignoredStatementsById.values
  def addIgnoredStatement(stmt: Statement): Unit =
    ignoredStatementsById.put(stmt.id, stmt)

  def avgClassesPerPackage = classCount / packageCount.toDouble
  def avgClassesPerPackageFormatted: String = DoubleFormat.twoFractionDigits(
    avgClassesPerPackage
  )

  def avgMethodsPerClass = methodCount / classCount.toDouble
  def avgMethodsPerClassFormatted: String = DoubleFormat.twoFractionDigits(
    avgMethodsPerClass
  )

  def loc = files.map(_.loc).sum
  def linesPerFile = loc / fileCount.toDouble
  def linesPerFileFormatted: String =
    DoubleFormat.twoFractionDigits(linesPerFile)

  // returns the classes by least coverage
  def risks(limit: Int) = classes.toSeq
    .sortBy(_.statementCount)
    .reverse
    .sortBy(_.statementCoverage)
    .take(limit)

  def apply(ids: Iterable[(Int, String)]): Unit = ids foreach invoked
  def invoked(id: (Int, String)): Unit =
    statementsById.get(id._1).foreach(_.invoked(id._2))
}

trait ClassBuilders {
  def statements: Iterable[Statement]
  def classes = statements
    .groupBy(_.location.fullClassName)
    .map(arg => MeasuredClass(arg._1, arg._2))
  def classCount: Int = classes.size
}

trait FileBuilders {
  def statements: Iterable[Statement]
  def files: Iterable[MeasuredFile] =
    statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
  def fileCount: Int = files.size
}

sealed trait ClassType
object ClassType {
  case object Object extends ClassType
  case object Class extends ClassType
  case object Trait extends ClassType
  def fromString(str: String): ClassType = {
    str.toLowerCase match {
      case "object" => Object
      case "trait"  => Trait
      case _        => Class
    }
  }
}

case class ClassRef(name: String) {
  lazy val simpleName = name.split(".").last
  lazy val getPackage = name.split(".").dropRight(1).mkString(".")
}

object ClassRef {
  def fromFilepath(path: String) = ClassRef(path.replace('/', '.'))
  def apply(_package: String, className: String): ClassRef = ClassRef(
    _package.replace('/', '.') + "." + className
  )
}
