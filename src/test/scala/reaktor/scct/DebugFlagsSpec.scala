package reaktor.scct

import org.specs2.mutable._
import reflect.internal.Flags

class DebugFlagsSpec extends Specification {
  "what are these" in {
    println("Flags: "+Flags.flagsToString(131136))
  }
}