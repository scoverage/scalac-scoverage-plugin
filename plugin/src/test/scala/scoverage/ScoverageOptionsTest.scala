package scoverage

import munit.FunSuite

class ScoverageOptionsTest extends FunSuite {

  val initalOptions = ScoverageOptions.default()
  val fakeOptions = List(
    "dataDir:myFakeDir",
    "measurementDir:myFakeMeasurementDir",
    "sourceRoot:myFakeSourceRoot",
    "excludedPackages:some.package;another.package*",
    "excludedFiles:*.proto;iHateThisFile.scala",
    "excludedSymbols:someSymbol;anotherSymbol;aThirdSymbol",
    "extraAfterPhase:extarAfter",
    "extraBeforePhase:extraBefore",
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
    assertEquals(parsed.measurementDir, Some("myFakeMeasurementDir"))
    assertEquals(parsed.reportTestName, true)
    assertEquals(parsed.sourceRoot, "myFakeSourceRoot")
  }

  test(
    "effectiveMeasurementDir should default to dataDir when measurementDir is not set"
  ) {
    val result =
      ScoverageOptions.parse(
        List("dataDir:myFakeDir"),
        (_) => (),
        initalOptions
      )
    assertEquals(result.measurementDir, None)
    assertEquals(result.effectiveMeasurementDir, "myFakeDir")
  }

  test(
    "effectiveMeasurementDir should use measurementDir when set"
  ) {
    assertEquals(parsed.effectiveMeasurementDir, "myFakeMeasurementDir")
  }

}
