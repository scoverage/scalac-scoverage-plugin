package reaktor.scct

import java.lang.String
import java.io.{FileFilter, FilenameFilter, File}
import report.HtmlReporter

object SelfTest extends InstrumentationSupport {
  def main(args: Array[String]) = {
    val src = findSources(new File("src/main/scala"))
    val plugin = compileFiles(src :_*)
    System.setProperty("scct.src.reference.dir", "src/main/scala")
    val reportDir = new File("self-report")
    reportDir.mkdir
    HtmlReporter.report(plugin.data, reportDir)
  }

  private def findSources(dir: File): List[String] = findSources(dir, dir)
  private def findSources(rootDir: File, dir: File): List[String] = {
    val files = dir.listFiles(new FileFilter() {
      def accept(f: File) = f.getName.endsWith(".scala")
    })
    val dirs = dir.listFiles(new FileFilter() {
      def accept(f: File) = f.isDirectory
    })
    files.toList.map(_.getAbsolutePath) ::: dirs.toList.map(findSources(rootDir, _)).flatten
  }
}