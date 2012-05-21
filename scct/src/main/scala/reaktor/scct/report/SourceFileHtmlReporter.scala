package reaktor.scct.report

import xml.{Unparsed, NodeSeq, Text}
import reaktor.scct._
import java.io.File
import annotation.tailrec

object SourceFileHtmlReporter {
  def report(sourceFile: String, data: CoverageData, project: ProjectData) = {
    val sourceName = cleanSourceName(sourceFile, project.baseDir, project.sourceDir)
    val lines = project.sourceLoader.linesFor(sourceFile)
    new SourceFileHtmlReporter(sourceName, data, lines).report
  }
  def cleanSourceName(sourceFile: String, baseDir:File, sourceDir:File) = {
    println("clean up: "+sourceFile + "  " + sourceDir.getAbsolutePath)
    val sourcePathSuffix = IO.relativePath(new File(baseDir, sourceFile), sourceDir)
    Some(sourcePathSuffix).map(_.replaceAll("//", "/")).map(s => if (s.startsWith("/")) s.substring(1) else s).get
  }
}

class SourceFileHtmlReporter(sourceFileName: String, data: CoverageData, lines: List[String]) extends HtmlHelper {

  val zeroSpace = Unparsed("&#x200B;")

  def report = {
    sourceFileTableHeader ++ sourceFileTableContent
  }

  def sourceFileTableHeader = {
    val header = itemRow(sourceFileHeader, data.percentage, "#")
    val classRows = classItemRows(data)
    <table class="classes"><tbody>{ header }{ classRows }</tbody></table>
  }

  def sourceFileHeader = {
    sourceFileName.lastIndexOf('/') match {
      case -1 => <span class="header">{ sourceFileName }</span>
      case idx => {
        val pkgName = sourceFileName.substring(0, idx+1)
        val fileName = sourceFileName.substring(idx+1)
        val packages = pkgName.split("/").foldLeft(NodeSeq.Empty) { (nodes, curr) => nodes ++ zeroSpace ++ Text(curr+"/") }
        packages ++ <span class="header">{ zeroSpace ++ Text(fileName) }</span>
      }
    }
  }

  def sourceFileTableContent =
    <table class="source"><tbody>{ sourceLines }</tbody></table>

  def sourceLines: NodeSeq = {
    sourceLines(1, 0, lines, data.blocks, List[Name](), NodeSeq.Empty)
  }

  @tailrec private def sourceLines(lineNum: Int, offset: Int, lines: List[String], blocks: List[CoveredBlock], usedNames: List[Name], acc: NodeSeq): NodeSeq = {
    lines match {
      case Nil => acc
      case line :: tail => {
        val maxOffset = offset + line.length
        val (currBlocks, nextBlocks) = blocks.partition(_.offset < maxOffset)
        val lineHtml = formatLine(line, offset, currBlocks.filter(!_.placeHolder))
        val newNames = currBlocks.map(_.name).filterNot(usedNames.contains).distinct
        val newNamesHtml = newNames.map(n => <a id={toHtmlId(n)}/>)
        val rowHtml =
          <tr>
            <td class={chooseColor(currBlocks.filter(!_.placeHolder))}>{lineNum}</td>
            <td>{ newNamesHtml }{ lineHtml }</td>
          </tr>
        sourceLines(lineNum + 1, maxOffset, tail, nextBlocks, usedNames ++ newNames, acc ++ rowHtml)
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

  @tailrec private def formatLine(prevChars: List[Char], line: List[Char], offset: Int, isCovered: Boolean, blocks: List[CoveredBlock], acc: NodeSeq): NodeSeq = {
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