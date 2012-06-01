package reaktor.scct.report

import java.io.File
import reaktor.scct.Env

object MultiProjectHtmlReporter {
  def report(files: List[File], multiProjectReportDir: File) {
    val env = new Env {
      override val reportDir = multiProjectReportDir
    }
    val writer = new HtmlReportWriter(env.reportDir)
    val projects: List[ProjectData] = files.map(BinaryReporter.read)

    val mergedProjectData = new ProjectData(env, projects.flatMap(_.coverage.blocks))

    val parentReporter = new HtmlReporter(mergedProjectData, writer)
    parentReporter.summaryReport
    parentReporter.packageListReport
    parentReporter.packageReports
    parentReporter.resources

    projects.foreach { p =>
      println("Reporting source for project: " + p)
      new HtmlReporter(p, writer).sourceFileReports
    }
  }
}
