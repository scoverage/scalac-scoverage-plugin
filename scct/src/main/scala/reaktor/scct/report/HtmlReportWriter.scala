package reaktor.scct.report

import java.io.File
import xml.NodeSeq
import reaktor.scct.IO

class HtmlReportWriter(outputDir: File) {
  lazy val template = IO.readResource("/html-reporting/template.html")

  def writeBody(fileName: String, body: NodeSeq) {
    writeBody(fileName, body, NodeSeq.Empty)
  }
  def writeBody(fileName: String, body: NodeSeq, head: NodeSeq) {
    writeTemplate(fileName, <body>{body}</body>, head)
  }
  def writeTemplate(fileName: String, body: NodeSeq, head: NodeSeq) {
    write(fileName, template.replace("<!-- @BODY@ -->", body.toString).replace("<!-- @HEAD@ -->", head.toString))
  }
  def write(fileName: String, content: String) {
    write(fileName, content.getBytes("utf-8"))
  }
  def write(fileName: String, bytes: Array[Byte]) {
    IO.write(new File(outputDir, fileName), bytes)
  }
}
