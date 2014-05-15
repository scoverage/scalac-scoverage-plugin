package scoverage

import java.io.FileWriter

/** @author Stephen Samuel */
object Invoker {

  val threadFile = new ThreadLocal[FileWriter]

  /**
   * We record that the given id has been invoked by appending its id to the coverage
   * data file.
   *
   * This will happen concurrently on as many threads as the application is using,
   * so we use one file per thread, named for the thread id.
   *
   * This method is not thread-safe if the threads are in different JVMs, because
   * the thread IDs may collide.
   * You may not use `scoverage` on multiple processes in parallel without risking
   * corruption of the measurement file.
   *
   * @param id the id of the statement that was invoked
   * @param dataDir the directory where the measurement data is held
   */
  def invoked(id: Int, dataDir: String) = {
    // Each thread writes to a separate measurement file, to reduce contention
    // and because file appends via FileWriter are not atomic on Windows.
    var writer = threadFile.get()
    if (writer == null) {
      val file = IOUtils.measurementFile(dataDir)
      writer = new FileWriter(file, true)
      threadFile.set(writer)
    }
    writer.append(id.toString + '\n').flush()
  }
}
