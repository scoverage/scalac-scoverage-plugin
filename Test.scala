class Test {

    case class Person(name: String, age: Int)
    def instantiation = {
        val name = "sammy" + System.currentTimeMillis()
        new Person(name, 34)
    }
}