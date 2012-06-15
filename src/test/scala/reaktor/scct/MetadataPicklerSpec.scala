package reaktor.scct

import org.specs.Specification
import java.io.File

class MetadataPicklerSpec extends Specification {
  import reaktor.scct.CoveredBlockGenerator._

  "Savin' an' loadin'" should {
    "Create new file, save object and load the object" in {
      val f = new File(System.getProperty("java.io.tmpdir"), "scct-"+System.currentTimeMillis+".tmp")
      f.deleteOnExit
      f.exists must beFalse
      MetadataPickler.toFile(List(block(0)), f)
      f.exists must beTrue
      val loaded = MetadataPickler.load(f)
      loaded must haveSize(1)
      val b = loaded(0)
      b.id mustEqual "0"
      b.name mustEqual Name("0", ClassTypes.Class, "0", "0", "0")
      b.offset mustEqual 0
      b.placeHolder must beFalse
    }
    "Overwrite file" in {
      val f = tmpFile
      MetadataPickler.toFile(List(block(0)), f)
      MetadataPickler.toFile(List(block(1)), f)
      MetadataPickler.load(f).map(_.id) mustEqual List("1")
    }
  }

  private def tmpFile = {
    val f = File.createTempFile("scct", ".tmp")
    f.deleteOnExit
    f
  }

}