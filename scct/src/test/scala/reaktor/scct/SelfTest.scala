package reaktor.scct

import java.lang.String
import java.io.{FileFilter, File}
import report.{CoberturaReporter, HtmlReporter}

/* When running in IntelliJ IDEA, needs basedir as %MODULE_DIR% */
object SelfTest extends InstrumentationSupport {
  def main(args: Array[String]) = {
    val src = findSources(new File("src/main/scala"))
    val plugin = compileFiles(src :_*)
    val env = new Env {
      override val sourceDir = new File("src/main/scala")
      override val reportDir = new File("self-report")
    }
    env.reportDir.mkdir
    HtmlReporter.report(plugin.scctComponent.data, env)
    CoberturaReporter.report(plugin.scctComponent.data, env)
    println("file://"+env.reportDir.getCanonicalPath+"/index.html")
  }

  private def findSources(dir: File): List[String] = findSources(dir, dir)
  private def findSources(rootDir: File, dir: File): List[String] = {
    val files = dir.listFiles(new FileFilter() {
      def accept(f: File) = f.getName.endsWith(".scala")
    })
    val dirs = dir.listFiles(new FileFilter() {
      def accept(f: File) = f.isDirectory
    })
    files.toList.map(_.getCanonicalPath) ::: dirs.toList.map(findSources(rootDir, _)).flatten
  }
}