package reaktor.scct.report

import java.io.File
import io.Source
import reaktor.scct.Env

class SourceLoader(env: Env) {
  def linesFor(sourceFile: String) = {
    val src = Source.fromFile(new File(env.sourceBaseDir, sourceFile))
    toLines(src)
  }


  def toLines(source: Source) = {
    def toLines(acc: List[String]): List[String] = {
      if (source.isEmpty) {
        acc.reverse
      } else {
        val line = source.takeWhile(_ != '\n').mkString
        toLines(line+"\n" :: acc)
      }
    }
    toLines(List())
  }
}

