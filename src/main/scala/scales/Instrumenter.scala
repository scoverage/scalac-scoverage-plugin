package scales

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.SourceFile

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

class Coverage {
    val statements = new ListBuffer[MeasuredStatement]

    def add(stmt: MeasuredStatement): Unit = statements.append(stmt)
    def invoked(id: Int): Unit = statements.find(_.id == id).foreach(_.invoked)

    def files = statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
    def packages: Iterable[MeasuredPackage] = statements.groupBy(_._package).map(arg => MeasuredPackage(arg._1, arg._2))
    def statementCoverage: Double = statements.count(_.count > 0) / statements.size.toDouble
}

case class MeasuredPackage(name: String, statements: Iterable[MeasuredStatement]) {
    def files = statements.groupBy(_.source).map(arg => MeasuredFile(arg._1, arg._2))
    def classes = statements.groupBy(_._class).map(arg => MeasuredClass(arg._1, arg._2))
}

case class MeasuredFile(source: SourceFile, statements: Iterable[MeasuredStatement]) {
    def classes = statements.groupBy(_._class).map(arg => MeasuredClass(arg._1, arg._2))
    def packages: Iterable[MeasuredPackage] = statements.groupBy(_._package).map(arg => MeasuredPackage(arg._1, arg._2))
    def statementCoverage: Double = statements.count(_.count > 0) / statements.size.toDouble
}

case class MeasuredClass(name: String, statements: Iterable[MeasuredStatement]) {
    def statementCoverage: Double = statements.count(_.count > 0) / statements.size.toDouble
}

case class MeasuredStatement(source: SourceFile, _package: String, _class: String, id: Int, start: Int, line: Int, var end: Int = -1) {
    var count = 0
    def invoked: Unit = count = count + 1
}