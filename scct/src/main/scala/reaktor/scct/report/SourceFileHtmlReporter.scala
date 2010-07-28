package reaktor.scct.report

import xml.{Unparsed, NodeSeq, Text}
import reaktor.scct.{Env, ClassTypes, Name, CoveredBlock}

object SourceFileHtmlReporter {
  def report(sourceFile: String, data: CoverageData, env: Env) =
    new SourceFileHtmlReporter(sourceFile, data, new SourceLoader(env), env).report
}

class SourceFileHtmlReporter(sourceFile: String, data: CoverageData, sourceLoader: SourceLoader, env: Env) {
  import HtmlReporter._

  val sourcePath = env.sourceDir.getAbsolutePath

  def report = {
    sourceFileTableHeader ++ sourceFileTableContent
  }

  def sourceFileTableHeader = {
    val header = itemRow(sourceFileHeader(sourceFile), data.percentage, "#")
    val classRows = classItemRows(data)
    <table class="classes"><tbody>{ header }{ classRows }</tbody></table>
  }

  def sourceFileHeader(sourceFile: String) = {
    val name = formatSourceFileName(sourceFile)
    name.lastIndexOf('/') match {
      case -1 => <span class="header">{ name }</span>
      case idx => Text(name.substring(0, idx+1)) ++ <span class="header">{ name.substring(idx+1) }</span>
    }
  }
  def formatSourceFileName(sourceFile: String) = {
    def trimRoot(s: String) = s.indexOf(sourcePath) match {
      case -1 => s
      case idx => s.substring(sourcePath.length + idx)
    }
    Some(sourceFile).map(trimRoot).map(_.replaceAll("//", "/")).map(s => if (s.startsWith("/")) s.substring(1) else s).get
  }

  def sourceFileTableContent =
    <table class="source"><tbody>{ sourceLines(sourceFile, data) }</tbody></table>

  def sourceLines(sourceFile: String, data: CoverageData): NodeSeq = {
    sourceLines(1, 0, sourceLoader.linesFor(sourceFile), data.blocks, Name("", ClassTypes.Root, "", ""), NodeSeq.Empty)
  }

  def sourceLines(lineNum: Int, offset: Int, lines: List[String], blocks: List[CoveredBlock], currentName: Name, acc: NodeSeq): NodeSeq = {
    lines match {
      case Nil => acc
      case line :: tail => {
        val maxOffset = offset + line.length + 1 // (+1 for newline at end)
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