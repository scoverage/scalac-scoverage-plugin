/**
 * Created by sam on 07/07/13.
 */
class Test {
    def noblock = println("hello")
    def block = {
        println("world")
    }
    def conditional = if (System.currentTimeMillis() > 0) println("normal time") else println("big bang")
}