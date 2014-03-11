package scoverage

import java.io.{File, FileWriter}

/** @author Stephen Samuel */
object Invoker {

  /**
   * We record that the given id has been invoked by appending its id to the coverage
   * data file.
   * This will happen concurrently on as many threads as the application is using,
   * but appending small amounts of data to a file is atomic on both POSIX and Windows
   * if it is a single write of a small enough string.
   *
   * @see http://stackoverflow.com/questions/1154446/is-file-append-atomic-in-unix
   * @see http://stackoverflow.com/questions/3032482/is-appending-to-a-file-atomic-with-windows-ntfs
   */
  def invoked(id: Int, path: String) = {
    val dir = new File(path)
    dir.mkdirs()
    val file = new File(path + "/" + Thread.currentThread.getId)
    val writer = new FileWriter(file, true)
    writer.append(id.toString + ';')
    writer.close()
  }
}
