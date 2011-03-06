package reaktor.scct

import org.specs.Specification
import reaktor.scct.ClassTypes.ClassType

class NameSpec extends Specification {
  "Class names compare" in {
    name(ClassTypes.Class, "a").compare(name(ClassTypes.Class, "b")) must be < 0
    name(ClassTypes.Class, "b").compare(name(ClassTypes.Class, "b")) mustEqual 0
  }
  "Class types compare" in {
    name(ClassTypes.Class).compare(name(ClassTypes.Object)) must be < 0
    name(ClassTypes.Object).compare(name(ClassTypes.Package)) must be < 0
    name(ClassTypes.Package).compare(name(ClassTypes.Root)) must be < 0
    name(ClassTypes.Root).compare(name(ClassTypes.Trait)) must be < 0
  }
  "Name comparison comes first" in {
    name(ClassTypes.Object, "z").compare(name(ClassTypes.Class, "a")) must be > 0
    name(ClassTypes.Class, "z").compare(name(ClassTypes.Object, "a")) must be > 0
  }

  private def name(t: ClassType):Name = name(t, "a")
  private def name(t: ClassType, n: String):Name = Name("s", t, "p", n)
}