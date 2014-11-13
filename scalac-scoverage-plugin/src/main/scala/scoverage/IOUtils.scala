package scoverage

import java.io._

import scala.collection.{Set, mutable}
import scala.io.Source
import scala.xml.{Node, Utility, XML}

/** @author Stephen Samuel */
object IOUtils {

  private val MeasurementsPrefix = "scoverage.measurements."
  private val CoverageFileName = "scoverage.coverage.xml"
  val DataDir = "scoverage-data"

  def clean(dataDir: File): Unit = findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))

  def coverageFile(dataDir: File): File = coverageFile(dataDir.getAbsolutePath)
  def coverageFile(dataDir: String): File = new File(dataDir, CoverageFileName)

  /**
   * @return the measurement file for the current thread.
   */
  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(MeasurementsPrefix)
  })

  /**
   * This method ensures that the output String has only
   * valid XML unicode characters as specified by the
   * XML 1.0 standard. For reference, please see
   * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
   * standard</a>. This method will return an empty
   * String if the input is null or empty.
   *
   * @param in The String whose non-valid characters we want to remove.
   * @return The in String, stripped of non-valid characters.
   * @see http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html
   *
   */
  def escape(in: String): String = {
    val out = new StringBuilder()
    for ( current <- Option(in).getOrElse("").toCharArray ) {
      if ((current == 0x9) || (current == 0xA) || (current == 0xD) ||
        ((current >= 0x20) && (current <= 0xD7FF)) ||
        ((current >= 0xE000) && (current <= 0xFFFD)) ||
        ((current >= 0x10000) && (current <= 0x10FFFF)))
        out.append(current)
    }
    out.mkString
  }

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

  /**
   * Write out coverage data to the given data directory
   */
  def serialize(coverage: Coverage, dataDir: String): Unit = serialize(coverage, coverageFile(dataDir))
  def serialize(coverage: Coverage, file: File): Unit = {
    val out = new BufferedWriter(new FileWriter(file))
    out.write(serialize(coverage).toString())
    out.close()
  }

  def serialize(coverage: Coverage): Node = {
    val lines = coverage.statements.map(stmt => {
      <statement>
        <source>
          {stmt.source}
        </source>
        <package>
          {stmt.location.packageName}
        </package>
        <class>
          {stmt.location.className}
        </class>
        <classType>
          {stmt.location.classType.toString}
        </classType>
        <method>
          {stmt.location.method}
        </method>
        <path>
          {stmt.location.sourcePath}
        </path>
        <id>
          {stmt.id.toString}
        </id>
        <start>
          {stmt.start.toString}
        </start>
        <end>
          {stmt.end.toString}
        </end>
        <line>
          {stmt.line.toString}
        </line>
        <description>
          {escape(stmt.desc)}
        </description>
        <symbolName>
          {escape(stmt.symbolName)}
        </symbolName>
        <treeName>
          {escape(stmt.treeName)}
        </treeName>
        <branch>
          {stmt.branch.toString}
        </branch>
        <count>
          {stmt.count.toString}
        </count>
      </statement>
    })
    Utility.trim(<statements>
      {lines}
    </statements>)
  }

  def deserialize(str: String): Coverage = {
    val xml = XML.loadString(str)
    val statements = xml \ "statement" map (node => {
      val source = (node \ "source").text
      val count = (node \ "count").text.toInt
      val branch = (node \ "branch").text.toBoolean
      val _package = (node \ "package").text
      val _class = (node \ "class").text
      val method = (node \ "method").text
      val path = (node \ "path").text
      val treeName = (node \ "treeName").text
      val symbolName = (node \ "symbolName").text
      val id = (node \ "id").text.toInt
      val line = (node \ "line").text.toInt
      val desc = (node \ "description").text
      val start = (node \ "start").text.toInt
      val end = (node \ "end").text.toInt
      val classType = (node \ "classType").text match {
        case "Trait" => ClassType.Trait
        case "Object" => ClassType.Object
        case _ => ClassType.Class
      }
      MeasuredStatement(source,
        Location(_package, _class, classType, method, path),
        id,
        start,
        end,
        line,
        desc,
        symbolName,
        treeName, branch, count)
    })

    val coverage = Coverage()
    for ( statement <- statements )
      coverage.add(statement)
    coverage
  }

  def deserialize(file: File): Coverage = deserialize(new FileReader(file))
  def deserialize(reader: Reader): Coverage = {
    val buffered = new BufferedReader(reader)
    val sb = new StringBuilder
    var line = buffered.readLine()
    while (line != null) {
      sb.append(line)
      line = buffered.readLine()
    }
    val coverage = deserialize(sb.toString())
    reader.close()
    coverage
  }
}
