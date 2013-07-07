package scales

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger

/** @author Stephen Samuel */
object Instrumentation {

    val instructions = mutable.Map[Int, MeasuredInstruction]()
    val ids = new AtomicInteger(0)

    def add(source: Option[String], start: Int, line: Int) = {
        val id = ids.incrementAndGet()
        val instruction = MeasuredInstruction(source.orNull, id, start, line)
        instructions.put(id, instruction)
        instruction
    }

    def invoked(id: Int) = {
        println("Hello from coverage: " + id)
        instructions.get(id).foreach(_.invoked)
        id
    }

    def instructionCoverage: Double = instructions.values.count(_.count > 0) / instructions.size
}

case class MeasuredInstruction(source: String, id: Int, start: Int, line: Int, var end: Int = -1) {
    var count = 0
    def invoked: Unit = count = count + 1
}