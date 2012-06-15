package reaktor.scct.report

import org.specs.Specification
import java.io.File

class BinaryReporterSpec extends Specification {
  import reaktor.scct.CoveredBlockGenerator._

  /*
    For sbt-wierdness-reasons, this works in IDEA but fails on sbt 0.77 with classloading errors. Giving up.

  doBeforeSpec {
    val f = new File(new File(System.getProperty("java.io.tmpdir", "/tmp")), BinaryReporter.dataFile)
    if (f.exists()) f.delete()
  }

  "Readin and riting" in {
    val tmp = new File(System.getProperty("java.io.tmpdir", "/tmp"))
    new File(tmp, BinaryReporter.dataFile).exists() must beFalse
    val projectData = ProjectData("myProject", new File("/baseDir"), new File("/sourceDir"), blocks(true, false))
    BinaryReporter.report(projectData, tmp)
    new File(tmp, BinaryReporter.dataFile).exists() must beTrue
    println("Plaze: "+tmp.getAbsolutePath)
    val result = BinaryReporter.read(new File(tmp, BinaryReporter.dataFile))
    result.projectId mustEqual "myProject"
    result.baseDir.getAbsolutePath mustEqual "/baseDir"
    result.sourceDir.getAbsolutePath mustEqual "/sourceDir"
    result.data must haveSize(2)
    result.data(0).id mustEqual "1"
    result.data(0).count mustEqual 1
    result.data(1).count mustEqual 0
  }

  */
}
