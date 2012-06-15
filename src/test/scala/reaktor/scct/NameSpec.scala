package reaktor.scct

import org.specs.Specification
import reaktor.scct.ClassTypes.ClassType

class NameSpec extends Specification {
  "Equality exists" in {
    name(ClassTypes.Class, "b", "p").compare(name(ClassTypes.Class, "b", "p")) mustEqual 0
  }
  "Class names compare" in {
    name(ClassTypes.Class, "a", "p").compare(name(ClassTypes.Class, "b", "p")) must be < 0
  }
  "Class types compare" in {
    name(ClassTypes.Class).compare(name(ClassTypes.Object)) must be < 0
    name(ClassTypes.Object).compare(name(ClassTypes.Package)) must be < 0
    name(ClassTypes.Package).compare(name(ClassTypes.Root)) must be < 0
    name(ClassTypes.Root).compare(name(ClassTypes.Trait)) must be < 0
  }
  "Project names compare" in {
    name(ClassTypes.Class, "x", "a").compare(name(ClassTypes.Class, "x", "b")) must be < 0
    name(ClassTypes.Class, "x", "z").compare(name(ClassTypes.Class, "x", "a")) must be > 0
  }
  "Name comparison comes first" in {
    name(ClassTypes.Object, "z", "a").compare(name(ClassTypes.Class, "a", "z")) must be > 0
    name(ClassTypes.Class, "z", "a").compare(name(ClassTypes.Object, "a", "z")) must be > 0
  }

  private def name(t: ClassType):Name = name(t, "a", "p")
  private def name(t: ClassType, n: String, p: String):Name = Name("s", t, "p", n, p)
}