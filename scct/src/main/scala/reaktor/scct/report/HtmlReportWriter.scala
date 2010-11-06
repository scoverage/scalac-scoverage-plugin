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
    val file = new File(outputDir, fileName)
    val parent = file.getParentFile
    if (!parent.exists) parent.mkdirs
    IO.write(file, bytes)
  }
}
