package reaktor.scct

import org.specs.Specification
import tools.nsc.symtab.Flags

class DebugFlagsSpec extends Specification {
  "what are these" in {
    println("Flags: "+Flags.flagsToString(131136))
  }
}