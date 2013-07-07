package scales

/** @author Stephen Samuel */
object Instrumenter {

    def go(id: Int) = {
        println("Hello from coverage: " + id)
        id
    }
}
