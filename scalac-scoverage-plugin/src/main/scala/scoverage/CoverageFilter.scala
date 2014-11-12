package scoverage

import scala.collection.mutable
import scala.reflect.internal.util.{Position, SourceFile}

/**
 * Methods related to filtering the instrumentation and coverage.
 *
 * @author Stephen Samuel
 */
trait CoverageFilter {
  def isClassIncluded(className: String): Boolean
  def isFileIncluded(file: SourceFile): Boolean
  def isLineIncluded(position: Position): Boolean
  def getExcludedLineNumbers(sourceFile: SourceFile): List[Range]
}

object AllCoverageFilter extends CoverageFilter {
  override def getExcludedLineNumbers(sourceFile: SourceFile): List[Range] = Nil
  override def isLineIncluded(position: Position): Boolean = true
  override def isClassIncluded(className: String): Boolean = true
  override def isFileIncluded(file: SourceFile): Boolean = true
}

class RegexCoverageFilter(excludedPackages: Seq[String],
                          excludedFiles: Seq[String]) extends CoverageFilter {

  val excludedClassNamePatterns = excludedPackages.map(_.r.pattern)
  val excludedFilePatterns = excludedFiles.map(_.r.pattern)

  /**
   * We cache the excluded ranges to avoid scanning the source code files
   * repeatedly. For a large project there might be a lot of source code
   * data, so we only hold a weak reference.
   */
  val linesExcludedByScoverageCommentsCache: mutable.Map[SourceFile, List[Range]] = mutable.WeakHashMap.empty

  final val scoverageExclusionCommentsRegex =
    """(?ms)^\s*//\s*(\$COVERAGE-OFF\$).*?(^\s*//\s*\$COVERAGE-ON\$|\Z)""".r

  /**
   * True if the given className has not been excluded by the
   * `excludedPackages` option.
   */
  override def isClassIncluded(className: String): Boolean = {
    excludedClassNamePatterns.isEmpty || !excludedClassNamePatterns.exists(_.matcher(className).matches)
  }

  override def isFileIncluded(file: SourceFile): Boolean = {
    def isFileMatch(file: SourceFile) = excludedFilePatterns.exists(_.matcher(file.path.replace(".scala", "")).matches)
    excludedFilePatterns.isEmpty || !isFileMatch(file)
  }

  /**
   * True if the line containing `position` has not been excluded by a magic comment.
   */
  def isLineIncluded(position: Position): Boolean = {
    if (position.isDefined) {
      val excludedLineNumbers = getExcludedLineNumbers(position.source)
      val lineNumber = position.line
      !excludedLineNumbers.exists(_.contains(lineNumber))
    } else {
      true
    }
  }

  /**
   * Checks the given sourceFile for any magic comments which exclude lines
   * from coverage. Returns a list of Ranges of lines that should be excluded.
   *
   * The line numbers returned are conventional 1-based line numbers (i.e. the
   * first line is line number 1)
   */
  def getExcludedLineNumbers(sourceFile: SourceFile): List[Range] = {
    linesExcludedByScoverageCommentsCache.get(sourceFile) match {
      case Some(lineNumbers) => lineNumbers
      case None =>
        val lineNumbers = scoverageExclusionCommentsRegex.findAllIn(sourceFile.content).matchData.map { m =>
          // Asking a SourceFile for the line number of the char after
          // the end of the file gives an exception
          val endChar = math.min(m.end(2), sourceFile.content.length - 1)
          // Most of the compiler API appears to use conventional
          // 1-based line numbers (e.g. "Position.line"), but it appears
          // that the "offsetToLine" method in SourceFile uses 0-based
          // line numbers
          Range(
            1 + sourceFile.offsetToLine(m.start(1)),
            1 + sourceFile.offsetToLine(endChar))
        }.toList
        linesExcludedByScoverageCommentsCache.put(sourceFile, lineNumbers)
        lineNumbers
    }
  }
}
