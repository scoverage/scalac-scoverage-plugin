package scoverage

import java.io.FileWriter

/** @author Stephen Samuel */
object Invoker {

  /**
   * We record that the given id has been invoked by appending its id to the coverage
   * data file.
   *
   * This will happen concurrently on as many threads as the application is using,
   * so this method is synchronized.
   *
   * This method is not thread-safe if the threads are in different JVMs. You may not
   * use `scoverage` on multiple processes in parallel without risking corruption
   * of the measurement file.
   */
  def invoked(id: Int, path: String) = {
    this.synchronized {
      val writer = new FileWriter(path, true)
      writer.write(id.toString + ';')
      writer.close()
    }
  }
}
