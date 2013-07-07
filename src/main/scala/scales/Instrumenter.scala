package scales

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer

/** @author Stephen Samuel */
object Instrumentation {

    val classes = mutable.Map[String, MeasuredClass]()
    val instructions = mutable.Map[Int, MeasuredInstruction]()
    val ids = new AtomicInteger(0)

    def add(source: String, start: Int, line: Int) = {
        val id = ids.incrementAndGet()
        val instruction = MeasuredInstruction(source, id, start, line)
        instructions.put(id, instruction)
        classes.get(source) match {
            case None => classes.put(source, new MeasuredClass(source))
            case _ =>
        }
        classes(source).add(instruction)
        instruction
    }

    def invoked(id: Int) = {
        println("Hello from coverage: " + id)
        instructions.get(id).foreach(_.invoked)
        id
    }

    def instructionCoverage(instructions: Iterable[MeasuredInstruction]): Double =
        instructions.count(_.count > 0) / instructions.size.toDouble
    def instructionCoverage: Double = instructions.values.count(_.count > 0) / instructions.size
}

case class MeasuredClass(name: String) {
    val instructions = new ListBuffer[MeasuredInstruction]
    def add(instr: MeasuredInstruction): Unit = instructions.append(instr)
}

case class MeasuredInstruction(source: String, id: Int, start: Int, line: Int, var end: Int = -1) {
    var count = 0
    def invoked: Unit = count = count + 1
}