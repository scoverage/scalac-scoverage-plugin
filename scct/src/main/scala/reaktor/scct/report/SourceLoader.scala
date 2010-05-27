package reaktor.scct.report

import java.io.File
import io.Source

class SourceLoader {
  val sourceRoot = new File(System.getProperty("scct.source.dir", ""))
  def linesFor(sourceFile: String) = Source.fromFile(new File(sourceRoot, sourceFile)).getLines.toList
}

