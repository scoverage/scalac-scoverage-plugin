package reaktor.scct.report

import java.io.File
import io.Source
import reaktor.scct.Env

class SourceLoader(env: Env) {
  def linesFor(sourceFile: String) = Source.fromFile(new File(env.sourceBaseDir, sourceFile)).getLines.toList
}

