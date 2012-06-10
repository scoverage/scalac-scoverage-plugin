package reaktor.scct

import java.lang.String
import java.io.{FileFilter, File}
import report.{HtmlReportWriter, ProjectData, CoberturaReporter, HtmlReporter}

/* When running in IntelliJ IDEA, needs basedir as %MODULE_DIR% */
object SelfTest extends InstrumentationSupport {
  def main(args: Array[String]) = {
    val src = findSources(new File("src/main/scala"))
    val plugin = compileFiles(src :_*)
    val env = new Env {
      override val baseDir = new File(".")
      override val sourceDir = new File("src/main/scala")
      override val reportDir = new File("self-report")
    }
    env.reportDir.mkdir
    val projectData = new ProjectData(env, plugin.scctComponent.data)
    val writer = new HtmlReportWriter(env.reportDir)
    new HtmlReporter(projectData, writer).report
    new CoberturaReporter(projectData, writer).report
    println("file://"+env.reportDir.getCanonicalPath+"/index.html")
  }

  private def findSources(dir: File): List[String] = {
    val files = dir.listFiles(new FileFilter() {
      def accept(f: File) = f.getName.endsWith(".scala")
    })
    val dirs = dir.listFiles(new FileFilter() {
      def accept(f: File) = f.isDirectory
    })
    files.toList.map(_.getCanonicalPath) ::: dirs.toList.map(findSources).flatten
  }
}