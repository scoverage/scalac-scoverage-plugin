package scales

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.SourceFile

/** @author Stephen Samuel */
object Instrumentation {

    val ids = new AtomicInteger(0)
    val coverage = new Coverage

    def add(source: SourceFile, owner: String, start: Int, line: Int) = {
        val id = ids.incrementAndGet()
        val instruction = MeasuredStatement(source, owner, id, start, line)
        coverage.instructions.put(id, instruction)
        coverage.files.get(source) match {
            case None => coverage.files.put(source, new MeasuredFile(source))
            case _ =>
        }
        coverage.files(source).add(instruction)
        instruction
    }

    def invoked(id: Int) = {
        coverage.instructions.get(id).foreach(_.invoked)
        id
    }
}

class Coverage {
    val files = mutable.Map[SourceFile, MeasuredFile]()
    val instructions = mutable.Map[Int, MeasuredStatement]()
    val packages = mutable.Map[String, MeasuredPackage]()

    // overall coverage
    def instructionCoverage(instructions: Iterable[MeasuredStatement]): Double =
        instructions.count(_.count > 0) / instructions.size.toDouble
}

case class MeasuredPackage

case class MeasuredFile(name: SourceFile) {
    val instructions = new ListBuffer[MeasuredStatement]
    def add(instr: MeasuredStatement): Unit = instructions.append(instr)
    def instructionCoverage: Double = instructions.count(_.count > 0) / instructions.size.toDouble
}

case class MeasuredStatement(source: SourceFile, owner: String, id: Int, start: Int, line: Int, var end: Int = -1) {
    var count = 0
    def invoked: Unit = count = count + 1
}