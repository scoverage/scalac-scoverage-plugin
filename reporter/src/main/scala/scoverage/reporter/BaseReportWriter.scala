package scoverage.reporter

import java.io.File

/** Abstract report writer.
  *
  * @param sourceRoots list of source directories
  * @param outputDir directory where to store the reports
  * @param outputEncoding encoding to use when writing files
  * @param recoverNoSourceRoot specifies how to handle source paths that are outside of the source roots.
  */
abstract class BaseReportWriter(
    sourceRoots: Seq[File],
    outputDir: File,
    outputEncoding: Option[String],
    recoverNoSourceRoot: BaseReportWriter.PathRecoverer
) {

  // Source paths in canonical form WITH trailing file separator
  private val formattedSourcePaths: Seq[String] =
    sourceRoots
      .filter(_.isDirectory)
      .map(_.getCanonicalPath + File.separatorChar)

  /** Converts an absolute path to a path relative to the reporter's source directories (aka "source roots").
    * If the path is not in the source roots, returns None.
    *
    * @param src absolute file path in canonical form
    * @return `Some(relativePath)` if `src` is in the source roots, else `None`
    */
  def relativeSource(src: String): Option[String] =
    relativeSource(src, formattedSourcePaths)

  private def relativeSource(
      src: String,
      sourceRoots: Seq[String]
  ): Option[String] = {
    // We need the canonical path for the given src because our formattedSourcePaths are canonical
    val canonicalSrc = new File(src).getCanonicalPath
    sourceRoots
      .find(root => canonicalSrc.startsWith(root))
      .map(root => canonicalSrc.substring(root.length))
      .orElse(recoverNoSourceRoot(new File(canonicalSrc), formattedSourcePaths))
  }
}
object BaseReportWriter {

  /** Specifies how to handle source path that are outside of the source roots.
    * Takes the source path (as a canonical File) and returns:
    * - `None` to skip the element
    * - `Some(newPath)` to use `newPath` instead
    *
    * The function may of course take additional actions, such as logging a warning,
    * throwing an error, etc.
    */
  type PathRecoverer = (File, Seq[String]) => Option[String]

  /** Throws an exception */
  def failIfNoSourceRoot(f: File, roots: Seq[String]): Option[String] =
    throw new RuntimeException(
      s"No source root found for '${f.getPath}' (source roots: $roots)"
    )
}
