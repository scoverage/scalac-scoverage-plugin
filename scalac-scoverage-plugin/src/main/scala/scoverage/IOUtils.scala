package scoverage

import java.io._

import scala.collection.{Set, mutable}
import scala.io.Source
import scala.xml.{Node, Utility, XML}

/** @author Stephen Samuel */
object IOUtils {

  private val MeasurementsPrefix = "scoverage.measurements."
  val DataDir = "scoverage-data"

  def clean(dataDir: File): Unit = findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))


  /**
   * @return the measurement file for the current thread.
   */
  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(MeasurementsPrefix)
  })

  def measurementFileSearch(baseDir: File): Array[File] = {
    def search(file: File): Array[File] = {

    }
    baseDir.listFiles().map {
      case file if file.isDirectory => search(file)
      case file if isMeasurementFile(file) => List(file)
    }
    search(baseDir)
  }

  val isMeasurementFile = (file: File) => file.getName.startsWith(MeasurementsPrefix)



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
