package scoverage

import java.io._
import scala.xml.{Utility, Node}

/** @author Stephen Samuel */
object IOUtils {

  private val MeasurementsPrefix = "scoverage.measurements."
  private val CoverageFileName = "scoverage.coverage"

  def coverageFile(dataDir: File): File = coverageFile(dataDir.getAbsolutePath)
  def coverageFile(dataDir: String): File = new File(dataDir + "/" + CoverageFileName)

  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir + "/" + MeasurementsPrefix + Thread.currentThread.getId)

  def findMeasurementFiles(dataDir: String): Array[File] = findMeasurementFiles(new File(dataDir))
  def findMeasurementFiles(dataDir: File): Array[File] = dataDir.listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.startsWith(MeasurementsPrefix)
  })

  // loads all the invoked statement ids from the given files
  def invoked(files: Seq[File]): Seq[Int] = {
    files.flatMap {
      file =>
      val reader = new BufferedReader(new FileReader(file))
      val line = reader.readLine()
      reader.close()
      line.split(";").filterNot(_.isEmpty).map(_.toInt)
    }
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
          {stmt.location._package}
        </package>
        <class>
          {stmt.location._class}
        </class>
        <classType>
          {stmt.location.classType.toString}
        </classType>
        <method>
          {stmt.location.method}
        </method>
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
          {stmt.desc}
        </description>
        <symbolName>
          {stmt.symbolName}
        </symbolName>
        <treeName>
          {stmt.treeName}
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

  def deserialize(classLoader: ClassLoader, file: File): Coverage = deserialize(classLoader, new FileInputStream(file))
  def deserialize(classLoader: ClassLoader, in: InputStream): Coverage = {
    val ois = new ClassLoaderObjectInputStream(classLoader, in)
    val obj = ois.readObject
    in.close()
    obj.asInstanceOf[Coverage]
  }
}

class ClassLoaderObjectInputStream(classLoader: ClassLoader, is: InputStream) extends ObjectInputStream(is) {
  override protected def resolveClass(objectStreamClass: ObjectStreamClass): Class[_] =
    try Class.forName(objectStreamClass.getName, false, classLoader) catch {
      case cnfe: ClassNotFoundException ⇒ super.resolveClass(objectStreamClass)
    }
}
