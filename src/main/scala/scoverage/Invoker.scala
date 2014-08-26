package scoverage

import java.io.FileWriter
import scala.collection.concurrent.TrieMap

/** @author Stephen Samuel */
object Invoker {

  private val threadFile = new ThreadLocal[FileWriter]
  private val ids = TrieMap.empty[Int, Any]

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
  def invoked(id: Int, dataDir: String): Unit = {
    // [sam] we can do this simple check to save writing out to a file.
    // This won't work across JVMs but since there's no harm in writing out the same id multiple
    // times since for coverage we only care about 1 or more, (it just slows things down to
    // do it more than once), anything we can do to help is good. This helps especially with code
    // that is executed many times quickly, eg tight loops.
    if (!ids.contains(id)) {
      // Each thread writes to a separate measurement file, to reduce contention
      // and because file appends via FileWriter are not atomic on Windows.
      var writer = threadFile.get()
      
      if (writer == null) {
        //When using scoverage in a multi-project maven build with dependencies between projects (eg: classes that inherit
        //from others in another project), this method gets called for classes higher up in the class hierarchy first.  If these
        //classes are in a different project from the one where the coverage data should be written, the writer should not
        //be set to write to that dataDir.  We assume that dependencies are built first and therefore we can recognize whether
        //or not the dataDir is correct by looking for already existent measurement files.
        val isNotInstrumented = IOUtils.findMeasurementFiles(dataDir).isEmpty
        if (isNotInstrumented) {
          val file = IOUtils.measurementFile(dataDir)
          writer = new FileWriter(file, true)
          threadFile.set(writer)
        }
      }

      if (writer != null) {
        writer.append(id.toString + '\n').flush()
        ids.put(id, ())
      }
    }
  }
}
