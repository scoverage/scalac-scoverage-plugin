package scoverage

import java.io._

import scala.collection.{Set, mutable}
import scala.io.Source
import scala.xml.{Node, Utility, XML}

/** @author Stephen Samuel */
object IOUtils {

  val MeasurementsPrefix = "scoverage.measurements."
  val DataDir = "scoverage-data"

  def clean(dataDir: File): Unit = findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))

  def writeToFile(file: File, str: String) = scala.tools.nsc.io.File(file.getAbsolutePath).writeAll(str)

  /**
   * @return the measurement file for the current thread.
   */
  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(MeasurementsPrefix)
  })

  def reportFileSearch(baseDir: File): Seq[File] = {
    def search(file: File): Seq[File] = file match {
      case dir if dir.isDirectory => dir.listFiles().toSeq.map(search).flatten
      case f if isReportFile(f) => Seq(f)
      case _ => Nil
    }
    search(baseDir)
  }

  val isMeasurementFile = (file: File) => file.getName.startsWith(MeasurementsPrefix)
  val isReportFile = (file: File) => file.getName == Constants.XMLReportFilename

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
