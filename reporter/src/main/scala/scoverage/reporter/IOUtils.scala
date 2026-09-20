package scoverage.reporter

import java.io._

import scala.collection.Set
import scala.collection.mutable
import scala.io.Codec
import scala.io.Source

import scoverage.domain.Constants

/** @author Stephen Samuel */
object IOUtils {

  def getTempDirectory: File = new File(getTempPath)
  def getTempPath: String = System.getProperty("java.io.tmpdir")

  def readStreamAsString(in: InputStream): String =
    Source.fromInputStream(in).mkString

  private val UnixSeperator: Char = '/'
  private val WindowsSeperator: Char = '\\'

  def getName(path: String): Any = {
    val index = {
      val lastUnixPos = path.lastIndexOf(UnixSeperator)
      val lastWindowsPos = path.lastIndexOf(WindowsSeperator)
      Math.max(lastUnixPos, lastWindowsPos)
    }
    path.drop(index + 1)
  }

  def reportFile(outputDir: File, debug: Boolean = false): File = debug match {
    case true  => new File(outputDir, Constants.XMLReportFilenameWithDebug)
    case false => new File(outputDir, Constants.XMLReportFilename)
  }

  def clean(dataDir: File): Unit =
    findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))

  def writeToFile(
      file: File,
      str: String,
      encoding: Option[String]
  ) = {
    val writer = new BufferedWriter(
      new OutputStreamWriter(
        new FileOutputStream(file),
        encoding.getOrElse(Codec.UTF8.name)
      )
    )
    try {
      writer.write(str)
    } finally {
      writer.close()
    }
  }

  /** Returns the measurement file for the current thread.
    */
  def measurementFile(dataDir: File): File = measurementFile(
    dataDir.getAbsolutePath
  )
  def measurementFile(dataDir: String): File =
    new File(dataDir, Constants.MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(
    new File(dataDir)
  )
  def findMeasurementFiles(dataDir: File): Array[File] =
    dataDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean =
        pathname.getName.startsWith(Constants.MeasurementsPrefix)
    })

  def scoverageDataDirsSearch(baseDir: File): Seq[File] = {
    def directoryFilter = new FileFilter {
      override def accept(pathname: File): Boolean = pathname.isDirectory
    }
    def search(file: File): Seq[File] = file match {
      case dir if dir.isDirectory && dir.getName == Constants.DataDir =>
        Seq(dir)
      case dir if dir.isDirectory =>
        dir.listFiles(directoryFilter).toSeq.flatMap(search)
      case _ => Nil
    }
    search(baseDir)
  }

  val isMeasurementFile = (file: File) =>
    file.getName.startsWith(Constants.MeasurementsPrefix)
  val isReportFile = (file: File) => file.getName == Constants.XMLReportFilename
  val isDebugReportFile = (file: File) =>
    file.getName == Constants.XMLReportFilenameWithDebug

  // loads all the invoked statement ids from the given files
  def invoked(
      files: Seq[File],
      encoding: String = Codec.UTF8.name
  ): Set[(Int, String)] = {
    val acc = mutable.Set[(Int, String)]()
    files.foreach { file =>
      val reader =
        Source.fromFile(file, encoding)
      for (line <- reader.getLines()) {
        if (!line.isEmpty) {
          acc += (line.split(" ").toList match {
            case List(idx, clazz) => (parseIdx(idx), clazz)
            case List(idx)        => (parseIdx(idx), "")
            // This should never really happen but to avoid a match error we'll default to a 0
            // index here since we start with 1 anyways.
            case _ => (0, "")
          })
        }
      }
      reader.close()
    }
    acc
  }

  // A measurement file is appended to by every instrumented thread/JVM that hits a statement
  // (see Invoker.invoked). Two lines written without an atomic append can interleave into one
  // token that still looks numeric but no longer fits in an Int (#476, e.g. "2849015114" >
  // Int.MaxValue). Same fallback the caller already uses for a line with the wrong number of
  // fields: don't fail the whole aggregation over one corrupted line.
  private def parseIdx(idx: String): Int =
    try idx.toInt
    catch { case _: NumberFormatException => 0 }

}
