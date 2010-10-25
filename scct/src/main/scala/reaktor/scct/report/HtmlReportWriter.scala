package reaktor.scct.report

import java.io.File
import xml.NodeSeq
import reaktor.scct.IO

class HtmlReportWriter(outputDir: File) {

  def write(fileName: String, nodes: NodeSeq) {
    write(fileName, nodes.toString)
  }
  def write(fileName: String, content: String) {
    write(fileName, content.getBytes("utf-8"))
  }
  def write(fileName: String, bytes: Array[Byte]) {
    if (!outputDir.exists) outputDir.mkdirs
    IO.write(new File(outputDir, fileName), bytes)
  }
}
