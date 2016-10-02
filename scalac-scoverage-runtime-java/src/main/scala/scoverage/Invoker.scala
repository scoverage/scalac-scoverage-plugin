package scoverage

object Invoker {

  // We explicitly call the java conversion else predef.scala will be used, and that may itself be instrumented.
  def invoked(id: Int, dataDir: String) = InvokerJ.invokedJ(java.lang.Integer.valueOf(id), dataDir)
}
