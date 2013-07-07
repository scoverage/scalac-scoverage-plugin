package scales

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger

/** @author Stephen Samuel */
object Instrumentation {

    val instructions = mutable.Map[Int, MeasuredInstruction]()
    val ids = new AtomicInteger(0)

    def add(start: Int) = {
        val id = ids.incrementAndGet()
        val instruction = MeasuredInstruction(id, start, -1)
        instructions.put(id, instruction)
        instruction
    }

    def invoked(id: Int) = {
        println("Hello from coverage: " + id)
        instructions.get(id).foreach(_.invoked)
        id
    }
}

case class MeasuredInstruction(id: Int, start: Int, var end: Int) {
    var count = 0
    def invoked: Unit = count = count + 1
}