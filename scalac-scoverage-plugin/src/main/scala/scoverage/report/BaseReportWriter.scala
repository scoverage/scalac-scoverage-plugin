package scoverage.report

import java.io.File

import scoverage.Coverage

class BaseReportWriter(sourceDirectories: Seq[File], outputDir: File, ignoreStatementsNotInSrcDirs: Boolean) {

  // Source paths in canonical form WITH trailing file separator
  private val formattedSourcePaths: Seq[String] = sourceDirectories filter ( _.isDirectory ) map ( _.getCanonicalPath + File.separator )

  /**
   * Converts absolute path to relative one if any of the source directories is it's parent.
   * If there is no parent directory, the path is returned unchanged (absolute).
   *
   * @param src absolute file path in canonical form
   */
  def relativeSource(src: String): String = relativeSource(src, formattedSourcePaths)

  private def relativeSource(src: String, sourcePaths: Seq[String]): String = {
    val sourceRoot: Option[String] = sourcePaths.find(
      sourcePath => src.startsWith(sourcePath)
    )
    sourceRoot match {
      case Some(path: String) => src.replace(path, "")
      case _ =>
        val fmtSourcePaths: String = sourcePaths.mkString("'", "', '", "'")
        throw new RuntimeException(s"No source root found for '$src' (source roots: $fmtSourcePaths)");
    }
  }

  def preprocessCoverage(coverage: Coverage): Coverage = {
    if (ignoreStatementsNotInSrcDirs)
      filteredCoverage(coverage)
    else
      coverage
  }

  /**
    * Filters out statements not in source roots
    * @return new Coverage instance with statements whose src paths are in root source paths.
    */
  private def filteredCoverage(coverage: Coverage): Coverage = {
    val filteredCoverage = Coverage()
    coverage.statements.foreach { stmt =>
      if (isInSourceRoots(stmt.source))
        filteredCoverage.add(stmt)
    }
    coverage.ignoredStatements.foreach { stmt =>
      if (isInSourceRoots(stmt.source))
        filteredCoverage.addIgnoredStatement(stmt)
    }
    filteredCoverage
  }

  private def isInSourceRoots(src: String): Boolean = {
    formattedSourcePaths.exists(sourcePath => src.startsWith(sourcePath))
  }

}
