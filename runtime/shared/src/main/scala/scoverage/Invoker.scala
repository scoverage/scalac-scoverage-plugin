package scoverage

import scala.collection.Set
import scala.collection.mutable
import scala.util.control.NonFatal

import scoverage.Platform._

/** @author Stephen Samuel */
object Invoker {

  private val runtimeUUID = Platform.insecureRandomUUID()

  private val MeasurementsPrefix = "scoverage.measurements."
  private val threadFiles =
    new ThreadLocal[mutable.HashMap[String, FileWriter]]

  // For each data directory we maintain a thread-safe set tracking the ids that we've already
  // seen and recorded. We're using a map as a set. The value is the recorded test name, or ""
  // when test names are not reported.
  private val dataDirToIds =
    ThreadSafeMap.empty[String, ThreadSafeMap[Int, Any]]

  @volatile private var shutdownHookInstalled = false

  /** We record that the given id has been invoked by appending its id to the coverage
    * data file.
    *
    * This will happen concurrently on as many threads as the application is using,
    * so we use one file per thread, named for the thread id.
    * Because each id is written at most once, a lost per-thread write cannot be
    * repaired by a later call.
    *
    * The in-memory id set is therefore also written to
    * `scoverage.measurements.<uuid>.shutdown` from a shutdown hook. The reporter unions
    * every measurement file, and duplicate ids do not change coverage.
    *
    * This method is not thread-safe if the threads are in different JVMs, because
    * the thread IDs may collide.
    * You may not use `scoverage` on multiple processes in parallel without risking
    * corruption of the measurement file.
    *
    * @param id the id of the statement that was invoked
    * @param dataDir the directory where the measurement data is held
    */
  def invoked(
      id: Int,
      dataDir: String,
      reportTestName: Boolean = false
  ): Unit = {
    ensureShutdownHook()
    // [sam] we can do this simple check to save writing out to a file.
    // This won't work across JVMs but since there's no harm in writing out the same id multiple
    // times since for coverage we only care about 1 or more, (it just slows things down to
    // do it more than once), anything we can do to help is good. This helps especially with code
    // that is executed many times quickly, eg tight loops.
    if (!dataDirToIds.contains(dataDir)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      dataDirToIds.synchronized {
        if (!dataDirToIds.contains(dataDir)) {
          dataDirToIds(dataDir) = ThreadSafeMap.empty[Int, Any]
        }
      }
    }
    val ids = dataDirToIds(dataDir)
    if (!ids.contains(id)) {
      // Each thread writes to a separate measurement file, to reduce contention
      // and because file appends via FileWriter are not atomic on Windows.
      val writer = writerFor(dataDir)
      val testName = if (reportTestName) getCallingScalaTest else ""

      if (reportTestName)
        writer
          .append(Integer.toString(id))
          .append(" ")
          .append(testName)
          .append("\n")
          .flush()
      else writer.append(Integer.toString(id)).append("\n").flush()
      ids.put(id, testName)
    }
  }

  private def ensureShutdownHook(): Unit =
    if (!shutdownHookInstalled) {
      synchronized {
        if (!shutdownHookInstalled) {
          Platform.onShutdown(() => dumpRecordedIds())
          shutdownHookInstalled = true
        }
      }
    }

  /** Write every id recorded in memory into a measurement file.
    *
    * Per-thread files are an optimization. This snapshot is what the reporter
    * can trust after the instrumented JVM exits: forked tests (the usual sbt
    * `coverage` / `test` / `coverageReport` sequence) read measurement files
    * only once that process has shut down.
    */
  private[scoverage] def dumpRecordedIds(): Unit =
    dataDirToIds.foreach { case (dataDir, ids) =>
      try {
        val dir = new File(dataDir)
        if (dir.isDirectory() && ids.nonEmpty) {
          val content = new mutable.StringBuilder
          ids.foreach { case (id, rawName) =>
            content.append(Integer.toString(id))
            rawName match {
              case name: String if name.nonEmpty =>
                content.append(" ").append(name)
              case _ =>
            }
            content.append('\n')
          }
          val writer = new FileWriter(
            new File(
              dataDir,
              MeasurementsPrefix + runtimeUUID + ".shutdown"
            ),
            true
          )
          try writer.append(content).flush()
          finally writer.close()
        }
      } catch {
        case NonFatal(_) => ()
      }
    }

  def getCallingScalaTest: String =
    Thread.currentThread.getStackTrace
      .map(_.getClassName.toLowerCase)
      .find(name =>
        name.endsWith("suite") || name.endsWith("spec") || name.endsWith("test")
      )
      .getOrElse("")

  def measurementFile(dataDir: File): File = measurementFile(
    dataDir.getAbsolutePath()
  )
  def measurementFile(dataDir: String): File = new File(
    dataDir,
    MeasurementsPrefix + runtimeUUID + "." + Thread.currentThread.getId
  )

  private def writerFor(dataDir: String): FileWriter = {
    var files = threadFiles.get()
    if (files == null) {
      files = mutable.HashMap.empty[String, FileWriter]
      threadFiles.set(files)
    }
    files.getOrElseUpdate(
      dataDir,
      new FileWriter(measurementFile(dataDir), true)
    )
  }

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(
    new File(dataDir)
  )
  def findMeasurementFiles(dataDir: File): Array[File] =
    dataDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean =
        pathname.getName().startsWith(MeasurementsPrefix)
    })

  // loads all the invoked statement ids from the given files
  def invoked(files: Seq[File]): Set[Int] = {
    val acc = mutable.Set[Int]()
    files.foreach { file =>
      val reader = Source.fromFile(file)
      for (line <- reader.getLines()) {
        if (!line.isEmpty) {
          acc += line.toInt
        }
      }
      reader.close()
    }
    acc
  }
}
