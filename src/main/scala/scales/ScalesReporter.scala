package scales

/** @author Stephen Samuel */
object ScalesReporter extends Reporter {
    def report(map: Map[String, MeasuredClass]) {
        for ( entry <- map )
            println(entry._2.name + " Coverage " + Instrumentation.instructionCoverage(entry._2.instructions))
    }
}

trait Reporter
