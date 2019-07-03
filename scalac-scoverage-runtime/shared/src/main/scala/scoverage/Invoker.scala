package scoverage

import scala.collection.{Set, mutable}
import java.nio.file.{Files, Paths, StandardCopyOption}
import scoverage.Platform._

/** @author Stephen Samuel */
object Invoker {

  private val MeasurementsPrefix = "scoverage.measurements."
  private val CoverageFileName = "scoverage.coverage"
  private val threadFiles = new ThreadLocal[mutable.HashMap[String, FileWriter]]
  // For each data directory we maintain a thread-safe set tracking the ids that we've already
  // seen and recorded. We're using a map as a set, so we only care about its keys and can ignore
  // its values.
  private val dataDirToIds = ThreadSafeMap.empty[String, ThreadSafeMap[Int, Any]]
  // environment variable from where we get the path to output dir for measurements
  private val measurementFileEnv = "SCOVERAGE_MEASUREMENT_PATH"
  // Used to store measurements files in case environment option is not set
  private val subdir = "scoverageMeasurements"


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
   * Same method as above but added additional features to make it work with
   * https://github.com/scoverage/scalac-scoverage-plugin/issues/265. Typically, we clean the dataDir
   * and add coverage instruments to the measurement file at runtime rather than compile time. This is
   * needed as ahe runtime process may be running on a different host than the compile time one.
   * However, since the instruments are on the classpath, we can retrieve the instruments from different hosts.
   *
   * @param id the id of the statement that was invoked
   * @param instrumentsDir the directory where the instrument data is held and needs to be copied from
   */
  def invokedWriteToClasspath(id: Int, instrumentsDir: String): Unit = {
    // Get the output data directory from the environment variable or write to a subdir.
    var dataDir: String = ""
    sys.env.get(measurementFileEnv) match {
      case Some(d) => dataDir = d
      case None => dataDir = subdir
    }

    // Generating a hash to add as a subdir to the basedir. This is because every
    // instrumented binary has a coverage object for which measurements will be generated
    // and it is possible that a test file tests multiple instrumented binaries. Thus, to
    // segregate reports for each source file, we need this hash. This was not an issue with
    // previous version as then we explicitly specified a different [dataDir] each time.
    dataDir += s"/${safe_name(instrumentsDir)}"
    // [sam] we can do this simple check to save writing out to a file.
    // This won't work across JVMs but since there's no harm in writing out the same id multiple
    // times since for coverage we only care about 1 or more, (it just slows things down to
    // do it more than once), anything we can do to help is good. This helps especially with code
    // that is executed many times quickly, eg tight loops.
    if (!dataDirToIds.contains(dataDir)) {
      // Guard against SI-7943: "TrieMap method getOrElseUpdate is not thread-safe".
      dataDirToIds.synchronized {
        if (!dataDirToIds.contains(dataDir)) {
          // When code is changed in source file, old measurements present
          // inside the dataDir can skew the results. Thus, clean the dataDir to remove
          // old measurements.
          resetDataDir(dataDir)
          // copy instruments.
          if (Files.exists(Paths.get(s"$instrumentsDir/$CoverageFileName"))) {
            copyCoverageFile(instrumentsDir, dataDir)
          }
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

  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(MeasurementsPrefix)
  })


  def resetDataDir(dataDir: String): Unit = {
    // Making the directory if it does not exist
    new File(dataDir).mkdirs()

    // clean the directory
    findMeasurementFiles(dataDir).foreach(_.delete)
  }

  /**
   *  Copy coverage file from [src] dir to [dest] dir
   * @param src directory path for source of coverage sfile
   * @param dest directory path for destination of coverage file
   */
  def copyCoverageFile(src: String, dest: String) = {
    Files.copy(
      Paths.get(s"$src/$CoverageFileName"),
      Paths.get(s"$dest/$CoverageFileName"),
      StandardCopyOption.REPLACE_EXISTING
    )
  }

  def safe_name(str: String): String =  {
    val pattern = "[^/]+".r
    val matches = pattern.findAllIn(str).toList
    val res = matches.mkString(".")
    res
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
