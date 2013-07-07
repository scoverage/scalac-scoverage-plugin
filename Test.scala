class Test {
    def noblock: Unit = println("hello")
    def block: Unit = {
        println("world")
    }
    def blockreturn = {
        println("with return")
        10
    }
    def conditional: Unit = if (System.currentTimeMillis() > 0) println("normal time") else println("big bang")
}