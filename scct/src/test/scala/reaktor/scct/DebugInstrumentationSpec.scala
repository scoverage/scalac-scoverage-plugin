package reaktor.scct

class DebugInstrumentationSpec extends InstrumentationSpec {

  "debug" in {
    offsetsMatch("class @MyAnnotation extends StaticAnnotation")
  }

  /*
  "test flags" in {
    println((131584 & tools.nsc.symtab.Flags.LABEL) != 0)
    println(tools.nsc.symtab.Flags.flagsToString(131584L))
  }
  */
}