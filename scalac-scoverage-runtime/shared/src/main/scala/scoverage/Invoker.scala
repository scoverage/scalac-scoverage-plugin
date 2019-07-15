package scoverage

import scala.collection.{Set, mutable}
import java.nio.file.{Files, Paths, StandardCopyOption}
import scoverage.Platform._

/** @author Stephen Samuel */
object Invoker {

  private val MeasurementsPrefix = "scoverage.measurements."
  private val CoverageFileName = "scoverage.coverage"
  private val ClasspathSubdir = "META-INF/scoverage"

  private val threadFiles = new ThreadLocal[mutable.HashMap[String, FileWriter]]
  // For each data directory we maintain a thread-safe set tracking the ids that we've already
  // seen and recorded. We're using a map as a set, so we only care about its keys and can ignore
  // its values.
  private val dataDirToIds = ThreadSafeMap.empty[String, ThreadSafeMap[Int, Any]]
  // System property from where we get the path to output dir for measurements
  private val measurementFileEnv = "scoverage_measurement_path"
  // Used to store measurements files in case system property is not set
  private val subdir = "scoverageMeasurements"

  // Get the output data directory from the System property [scoverage_measurement_path]
  // variable or write to a subdir.
  lazy val dataDir: String = System.getProperty(measurementFileEnv, subdir)


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
      var files = threadFiles.get()
      if (files == null) {
        files = mutable.HashMap.empty[String, FileWriter]
        threadFiles.set(files)
      }
      val writer = files.getOrElseUpdate(dataDir, new FileWriter(measurementFile(dataDir), true))
      writer.append(Integer.toString(id)).append("\n").flush()

      ids.put(id, ())
    }
  }

  /**
   * Invokes above method after adding additional features to make it work with
   * https://github.com/scoverage/scalac-scoverage-plugin/issues/265. Typically, we
   * add coverage instrument files [scoverage.coverage] along with the measurement files at runtime
   * rather than compile time. This is needed as the runtime process may be running
   * on a different host than the compile time one.
   * However, since the instrument files are on the classpath, we can retrieve the instruments from different hosts.
   *
   * @param id the id of the statement that was invoked
   * @param targetId targetId helps in grabbing the "correct" instrument file from cp that corresponds
   *                 to the measurement file written during this invocation.
   */
  def invokedWriteToClasspath(id: Int, targetId: String): Unit = {
    // Adding subdir to the basedir. This is because every
    // unique target has a instrument file [scoverage.coverage] for which corresponding
    // measurements [scoverage.measurements] will be generated and it is possible that a
    // test file tests multiple targets. And thus, multiple measurement files will be generated
    // which needs to be stored SEPARATELY from each other (but along with THEIR instrument files).
    // Thus, to segregate (instruments + measurements) for each target, we need this subdir.
    // This was not an issue with previous version as then we explicitly
    // specified a different [dataDir] each time we compiled a target.
    val newDataDir = dataDir + s"/$targetId"

    if (!dataDirToIds.contains(newDataDir)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      dataDirToIds.synchronized {
        if (!dataDirToIds.contains(newDataDir)) {
          // copy instruments.
          new File(newDataDir).mkdirs()
          copyCoverageFile(targetId, newDataDir)
          dataDirToIds(newDataDir) = ThreadSafeMap.empty[Int, Any]
        }
      }
    }
    invoked(id, newDataDir)
  }

  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(MeasurementsPrefix)
  })


  /**
   * Copy instruments file from classpath.
   *
   * @param targetId targetId helps in grabbing the "correct" instrument file from cp that corresponds
   *                 to the measurement file written during this invocation.
   * @param dest     directory path for destination of coverage file
   */
  def copyCoverageFile(targetId: String, dest: String): Unit = {

    // Getting the instrument file `META-INF/scoverage/< targetId >/scoverage.coverage` from classpath
    val r = Thread.currentThread.getContextClassLoader.getResourceAsStream(s"$ClasspathSubdir/$targetId/$CoverageFileName")

    // Copying the instruments file to the directory that will contain measurements.
    if (r != null) {
      Files.copy(r, Paths.get(s"$dest/$CoverageFileName"), StandardCopyOption.REPLACE_EXISTING)
      r.close()
    }
  }

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
