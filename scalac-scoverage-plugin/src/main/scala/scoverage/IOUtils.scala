package scoverage

import java.io._

import scala.collection.{Set, mutable}
import scala.io.Source

/** @author Stephen Samuel */
object IOUtils {

  def getTempDirectory: File = new File(getTempPath)
  def getTempPath: String = System.getProperty("java.io.tmpdir")

  def readStreamAsString(in: InputStream): String = Source.fromInputStream(in).mkString

  private val UnixSeperator: Char = '/'
  private val WindowsSeperator: Char = '\\'
  private val UTF8Encoding: String = "UTF-8"

  def getName(path: String): Any = {
    val index = {
      val lastUnixPos = path.lastIndexOf(UnixSeperator)
      val lastWindowsPos = path.lastIndexOf(WindowsSeperator)
      Math.max(lastUnixPos, lastWindowsPos)
    }
    path.drop(index + 1)
  }

  def reportFile(outputDir: File, debug: Boolean = false): File = debug match {
    case true => new File(outputDir, Constants.XMLReportFilenameWithDebug)
    case false => new File(outputDir, Constants.XMLReportFilename)
  }

  def clean(dataDir: File): Unit = findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))

  def writeToFile(file: File, str: String) = {
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UTF8Encoding))
    try {
      writer.write(str)
    } finally {
      writer.close()
    }
  }

  /**
   * Returns the measurement file for the current thread.
   */
  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, Constants.MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(Constants.MeasurementsPrefix)
  })

  def reportFileSearch(baseDir: File, condition: File => Boolean): Seq[File] = {
    def search(file: File): Seq[File] = file match {
      case dir if dir.isDirectory => dir.listFiles().toSeq.map(search).flatten
      case f if isReportFile(f) => Seq(f)
      case _ => Nil
    }
    search(baseDir)
  }

  val isMeasurementFile = (file: File) => file.getName.startsWith(Constants.MeasurementsPrefix)
  val isReportFile = (file: File) => file.getName == Constants.XMLReportFilename
  val isDebugReportFile = (file: File) => file.getName == Constants.XMLReportFilenameWithDebug

  // loads all the invoked statement ids from the given files
  def invoked(files: Seq[File]): Set[Int] = {
    val acc = mutable.Set[Int]()
    files.foreach { file =>
      val reader = Source.fromFile(file)
      for ( line <- reader.getLines() ) {
        if (!line.isEmpty) {
          acc += line.toInt
        }
      }
      reader.close()
    }
    acc
  }

}
