package reaktor.scct.report

import reaktor.scct.IO
import java.io._

object BinaryReporter {
  def report(projectData: ProjectData, reportDir: File) {
    val f = new File(reportDir, "coverage-result.data")
    IO.writeObjects(f) { _.writeObject(projectData) }
  }

  def read(reportDir: File): ProjectData = {
    val f = new File(reportDir, "coverage-result.data")
    IO.readObjects(f) { _.readObject().asInstanceOf[ProjectData] }
  }
}
