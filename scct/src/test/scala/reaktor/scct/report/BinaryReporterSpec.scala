package reaktor.scct.report

import org.specs.Specification
import java.io.File

class BinaryReporterSpec extends Specification {
  import reaktor.scct.CoveredBlockGenerator._

  "Readin and riting" in {
    val tmp = new File(System.getProperty("java.io.tmpdir", "/tmp"))
    val projectData = ProjectData("myProject", new File("/baseDir"), new File("/sourceDir"), blocks(true, false))
    BinaryReporter.report(projectData, tmp)
    val result = BinaryReporter.read(tmp)
    result.projectId mustEqual "myProject"
    result.baseDir.getAbsolutePath mustEqual "/baseDir"
    result.sourceDir.getAbsolutePath mustEqual "/sourceDir"
    result.data must haveSize(2)
    result.data(0).id mustEqual "1"
    result.data(0).count mustEqual 1
    result.data(1).count mustEqual 0
  }
}
