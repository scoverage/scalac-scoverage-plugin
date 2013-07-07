package scales

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.SourceFile
import scala.collection.mutable

/** @author Stephen Samuel */
object Instrumentation {

    val ids = new AtomicInteger(0)
    val coverage = new Coverage

    def add(source: SourceFile, _package: String, _class: String, start: Int, line: Int) = {
        val id = ids.incrementAndGet()
        val stmt = MeasuredStatement(source, _package, _class, id, start, line)
        coverage.add(stmt)
        stmt
    }

    def invoked(id: Int): Int = {
        coverage.invoked(id)
        id
    }
}

sealed trait LineStatus
case object Covered extends LineStatus
case object MissingCoverage extends LineStatus
case object NotInstrumented extends LineStatus

class Coverage {

    val statements = new ListBuffer[MeasuredStatement]
    val sources = mutable.Set[SourceFile]()
    val loc = sources.map(src => new String(src.content).count(_ == '\n')).sum
    val ncloc = sources.map(src => new String(src.content).replaceAll("/\\*.*?\\*/", "").count(_ == '\n')).sum
    val packageNames = mutable.Set[String]()
    def packageCount = packageNames.size
    val classNames = new ListBuffer[String]()
    def classCount = classNames.size
    val methodNames = new ListBuffer[String]()
    def methodCount = methodNames.size
    def classesPerPackage = classCount / packageCount.toDouble
    def methodsPerClass = methodCount / classCount.toDouble

    def add(stmt: MeasuredStatement): Unit = statements.append(stmt)
    def invoked(id: Int): Unit = statements.find(_.id == id).foreach(_.invoked)

    def files = statements.groupBy(_.source.path).map(arg => MeasuredFile(arg._2.head.source, arg._2))
    def packages: Iterable[MeasuredPackage] = statements.groupBy(_._package).map(arg => MeasuredPackage(arg._1, arg._2))
    def statementCoverage: Double = statements.count(_.count > 0) / statements.size.toDouble
    def statementCount = statements.size
}

case class MeasuredPackage(name: String, statements: Iterable[MeasuredStatement]) {
    def files = statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
    def classes = statements.groupBy(_._class).map(arg => MeasuredClass(arg._1, arg._2))
}

case class MeasuredFile(source: SourceFile, statements: Iterable[MeasuredStatement]) {

    def lineStatus(lineNumber: Int): LineStatus = {
        statements.filter(_.line == lineNumber) match {
            case i if i.isEmpty => NotInstrumented
            case i if i.size > 0 && i.exists(_.count == 0) => MissingCoverage
            case _ => Covered
        }
    }

    def classes = statements.groupBy(_._class).map(arg => MeasuredClass(arg._1, arg._2))
    def packages: Iterable[MeasuredPackage] = statements.groupBy(_._package).map(arg => MeasuredPackage(arg._1, arg._2))

    def totalStatements = statements.size
    def invokedStatements = statements.count(_.count > 0)
    def statementCoverage: Double = statements.count(_.count > 0) / statements.size.toDouble
}

case class MeasuredClass(name: String, statements: Iterable[MeasuredStatement]) {
    def statementCoverage: Double = statements.count(_.count > 0) / statements.size.toDouble
}

case class MeasuredStatement(source: SourceFile, _package: String, _class: String, id: Int, start: Int, line: Int, var end: Int = -1) {
    var count = 0
    def invoked: Unit = count = count + 1
}