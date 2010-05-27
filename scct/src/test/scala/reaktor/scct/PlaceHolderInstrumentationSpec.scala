package reaktor.scct

class PlaceHolderInstrumentationSpec extends InstrumentationSpec {
  "placeholders" should {
    "be inserted at class definitions" in {
      placeHoldersMatch("class @Bar { class @Foo {}; object @Baz {}; trait @Zab {}; case class @Zip; case object @Zob; }")
    }
    "even when code should be ignored" in {
      placeHoldersMatch("\\@reaktor.scct.uncovered class @Bar { class @Foo {} }")
    }
  }
}