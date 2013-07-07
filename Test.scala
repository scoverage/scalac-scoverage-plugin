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

    def switch: String = {
        val age = 20
        age match {
            case 10 => "boy"
            case 20 => "young adult"
            case 30 => "man"
        }
    }

    def tryclause: Int = {
        println("Preparing for int")
        var name = "padding"
        try {
            "qweqwe".toInt
        } catch {
            case e: NumberFormatException =>
                println("Could not change to number")
                0
            case e: Exception =>
                println("Fancy exception")
                -1
        }
    }
}