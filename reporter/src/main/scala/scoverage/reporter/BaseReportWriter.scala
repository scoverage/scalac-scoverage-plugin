package scoverage.reporter

import java.io.File

class BaseReportWriter(
    sourceDirectories: Seq[File],
    outputDir: File,
    sourceEncoding: Option[String]
) {

  // Source paths in canonical form WITH trailing file separator
  private val formattedSourcePaths: Seq[String] =
    sourceDirectories
      .filter(_.isDirectory)
      .map(_.getCanonicalPath + File.separator)

  /** Converts absolute path to relative one if any of the source directories is it's parent.
    * If there is no parent directory, the path is returned unchanged (absolute).
    *
    * @param src absolute file path in canonical form
    */
  def relativeSource(src: String): String =
    relativeSource(src, formattedSourcePaths)

  private def relativeSource(src: String, sourcePaths: Seq[String]): String = {
    // We need the canonical path for the given src because our formattedSourcePaths are canonical
    val canonicalSrc = new File(src).getCanonicalPath
    val sourceRoot: Option[String] =
      sourcePaths.find(sourcePath => canonicalSrc.startsWith(sourcePath))
    sourceRoot match {
      case Some(path: String) => canonicalSrc.replace(path, "")
      case _ =>
        val fmtSourcePaths: String = sourcePaths.mkString("'", "', '", "'")
        throw new RuntimeException(
          s"No source root found for '$canonicalSrc' (source roots: $fmtSourcePaths)"
        );
    }
  }

}
