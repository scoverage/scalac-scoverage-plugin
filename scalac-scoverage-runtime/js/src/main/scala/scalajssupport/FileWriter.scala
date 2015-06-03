package scalajssupport

/**
 * Emulates a subset of the java.io.FileWriter API required for scoverage to work.
 */
class FileWriter(file: File, append: Boolean) {
  def this(file: File) = this(file, false)
  def this(file: String) = this(new File(file), false)
  def this(file: String, append: Boolean) = this(new File(file), append)

  def append(csq: CharSequence) = {
    File.write(file.getPath, csq.toString)
    this
  }

  def close(): Unit = {
    // do nothing as we don't open a FD to the file, as phantomJS does not use FDs
  }

  override def finalize(): Unit = close()

  def flush() = {}
}
