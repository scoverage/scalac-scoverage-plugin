package reaktor.scct.report

import reaktor.scct.IO
import java.io._

object BinaryReporter {
  val dataFile = "coverage-result.data"
  def report(projectData: ProjectData, reportDir: File) {
    val f = new File(reportDir, dataFile)
    IO.writeObjects(f) { _.writeObject(projectData) }
  }

  def read(reportFile: File): ProjectData = {
    IO.readObjects(reportFile) { _.readObject().asInstanceOf[ProjectData] }
  }
}
