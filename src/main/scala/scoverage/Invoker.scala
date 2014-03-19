package scoverage

import java.io.{File, FileWriter}

/** @author Stephen Samuel */
object Invoker {

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
   */
  def invoked(id: Int, path: String) = {
    // Each thread writes to a separate measurement file, to reduce contention
    // and because file appends via FileWriter are not atomic on Windows.
    val file = new File(path, Thread.currentThread.getId.toString)
    val writer = new FileWriter(file, true)
    writer.append(id.toString + ';')
    writer.close()
  }
}
