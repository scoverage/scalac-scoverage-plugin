package scoverage

import munit.FunSuite

class ScoverageOptionsTest extends FunSuite {

  val initalOptions = ScoverageOptions.default()
  val fakeOptions = List(
    "dataDir:myFakeDir",
    "sourceRoot:myFakeSourceRoot",
    "excludedPackages:some.package;another.package*",
    "excludedFiles:*.proto;iHateThisFile.scala",
    "excludedSymbols:someSymbol;anotherSymbol;aThirdSymbol",
    "extraAfterPhase:extarAfter;extraAfter2",
    "extraBeforePhase:extraBefore;extraBefore2",
    "reportTestName"
  )

  val parsed = ScoverageOptions.parse(fakeOptions, (_) => (), initalOptions)

  test("should be able to parse all options") {
    assertEquals(
      parsed.excludedPackages,
      Seq("some.package", "another.package*")
    )
    assertEquals(parsed.excludedFiles, Seq("*.proto", "iHateThisFile.scala"))
    assertEquals(
      parsed.excludedSymbols,
      Seq("someSymbol", "anotherSymbol", "aThirdSymbol")
    )
    assertEquals(parsed.dataDir, "myFakeDir")
    assertEquals(parsed.reportTestName, true)
    assertEquals(parsed.sourceRoot, "myFakeSourceRoot")
  }

}
