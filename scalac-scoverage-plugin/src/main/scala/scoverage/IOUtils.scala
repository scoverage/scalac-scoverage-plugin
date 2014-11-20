package scoverage

import java.io._

import scoverage.report.ScoverageXmlMerger

import scala.collection.{Set, mutable}
import scala.io.Source

/** @author Stephen Samuel */
object IOUtils {

  def readStreamAsString(in: InputStream): String = Source.fromInputStream(in).mkString

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

  def clean(dataDir: File): Unit = findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))

  def writeToFile(file: File, str: String) = {
    val writer = new BufferedWriter(new FileWriter(file))
    try {
      writer.write(str)
    } finally {
      writer.close()
    }
  }

  /**
   * @return the measurement file for the current thread.
   */
  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, Constants.MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(Constants.MeasurementsPrefix)
  })

  def reportFileSearch(baseDir: File): Seq[File] = {
    def search(file: File): Seq[File] = file match {
      case dir if dir.isDirectory => dir.listFiles().toSeq.map(search).flatten
      case f if isReportFile(f) => Seq(f)
      case _ => Nil
    }
    search(baseDir)
  }

  /**
   * Aggregates all subproject reports, returning the location of the aggregated file.
   */
  val aggregator: (File, File) => File = (baseDir, targetDir) => {
    val files = IOUtils.reportFileSearch(baseDir)
    println(s"[info] Found ${files.size} subproject report files [${files.mkString(",")}]")
    val nodes = files.map(xml.XML.loadFile)
    val aggregated = ScoverageXmlMerger.merge(nodes)
    val outFile = new File(targetDir, Constants.XMLReportFilename)
    writeToFile(outFile, aggregated.toString)
    outFile
  }

  val isMeasurementFile = (file: File) => file.getName.startsWith(Constants.MeasurementsPrefix)
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
