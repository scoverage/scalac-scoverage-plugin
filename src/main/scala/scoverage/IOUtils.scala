package scoverage

import java.io._

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
    val out = new FileOutputStream(file)
    out.write(serialize(coverage))
    out.close()
  }
  def serialize(coverage: Coverage): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val out = new ObjectOutputStream(bos)
    out.writeObject(coverage)
    out.close()
    bos.toByteArray
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
