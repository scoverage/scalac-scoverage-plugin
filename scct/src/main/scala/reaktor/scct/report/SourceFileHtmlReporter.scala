package reaktor.scct.report

import xml.{NodeSeq, Text}
import reaktor.scct.{ClassTypes, Name, CoveredBlock}

object SourceFileHtmlReporter {
  def report(sourceFile: String, data: CoverageData) =
    new SourceFileHtmlReporter(sourceFile, data, new SourceLoader).report
}

class SourceFileHtmlReporter(sourceFile: String, data: CoverageData, sourceLoader: SourceLoader) {
  import HtmlReporter._

  val sourceReferenceDir = System.getProperty("scct.src.reference.dir", "")

  def report = {
    sourceFileHeader ++ sourceFileContent
  }

  def sourceFileHeader = {
    val header = itemRow(formatSourceFileName(sourceFile), data.percentage, "#")
    val classRows = classItemRows(data)
    table(header, classRows)
  }

  def formatSourceFileName(sourceFile: String) = {
    val name = Some(sourceFile).map(_.replaceFirst(sourceReferenceDir, "")).map(s => if (s.startsWith("/")) s.substring(1) else s).get
    name.lastIndexOf('/') match {
      case -1 => <span class="header">{ name }</span>
      case idx => Text(name.substring(0, idx+1)) ++ <span class="header">{ name.substring(idx+1) }</span>
    }
  }
  def sourceFileContent =
    <table class="source"><tbody>{ sourceLines(sourceFile, data) }</tbody></table>

  def sourceLines(sourceFile: String, data: CoverageData): NodeSeq = {
    sourceLines(1, 0, sourceLoader.linesFor(sourceFile), data.blocks, Name("", ClassTypes.Root, "", ""), NodeSeq.Empty)
  }

  def sourceLines(lineNum: Int, offset: Int, lines: List[String], blocks: List[CoveredBlock], currentName: Name, acc: NodeSeq): NodeSeq = {
    lines match {
      case Nil => acc
      case line :: tail => {
        val maxOffset = offset + line.length
        val (currBlocks, nextBlocks) = blocks.partition(_.offset < maxOffset)
        val lineHtml = formatLine(line, offset, currBlocks.filter(!_.placeHolder))
        val newName = currBlocks.headOption.map(_.name).getOrElse(currentName)
        val classId = if (currentName != newName) Some(Text(toHtmlId(newName))) else None
        val rowHtml =
          <tr id={classId}>
            <td class={chooseColor(currBlocks.filter(!_.placeHolder))}>{lineNum}</td>
            <td>{ lineHtml }</td>
          </tr>
        sourceLines(lineNum + 1, maxOffset, tail, nextBlocks, newName, acc ++ rowHtml)
      }
    }
  }

  private def chooseColor(metadatas: List[CoveredBlock]) = {
    metadatas.length match {
      case 0 => "black"
      case _ => {
        val hits = metadatas.filter(_.count > 0).size
        if (hits == 0) "red" else if (hits == metadatas.length) "green" else "yellow"
      }
    }
  }

  def formatLine(line: String, offset: Int, blocks: List[CoveredBlock]): NodeSeq =
    formatLine(List[Char](), line.toList, offset, true, blocks, NodeSeq.Empty)

  private def formatLine(prevChars: List[Char], line: List[Char], offset: Int, isCovered: Boolean, blocks: List[CoveredBlock], acc: NodeSeq): NodeSeq = {
    blocks match {
      case Nil => acc ++ formatLinePart(prevChars ::: line, isCovered)
      case block :: tail => {
        val (currChars, nextChars) = line.splitAt(block.offset - offset)
        if (isCovered == block.count > 0) {
          formatLine(prevChars ::: currChars, nextChars, block.offset, isCovered, tail, acc)
        } else {
          val part = formatLinePart(prevChars ::: currChars, isCovered)
          formatLine(List[Char](), nextChars, block.offset, block.count > 0, tail, acc ++ part)
        }
      }
    }
  }

  private def formatLinePart(part: List[Char], isCovered: Boolean) = {
    val nbsp = "\u00A0\u00A0"
    val tabbed = part.mkString.replaceAll("\t", nbsp).replaceAll("  ", nbsp)
    if (isCovered) Text(tabbed) else <span class="non">{ tabbed }</span>
  }

}