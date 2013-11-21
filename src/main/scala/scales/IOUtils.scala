package scales

import java.io._

/** @author Stephen Samuel */
object IOUtils {

  def dropScalaPath(path: String) = path.split("src/main/scala").last

  def toOutputPath(path: String) = "target/scales/" + dropScalaPath(path)
  def createDirs(path: String) = new File(path).isDirectory match {
    case false => new File(path).getParentFile.mkdirs
    case true => new File(path).mkdirs
  }

  // write to relative path
  def write(path: String, data: AnyRef) {
    val outputPath = toOutputPath(path)
    createDirs(outputPath)
    println(s"Writing to path $outputPath")
    val file = new File(outputPath)
    val writer = new BufferedWriter(new FileWriter(file))
    writer.write(data.toString)
    writer.close()
  }

  // loads all the invoked statement ids
  def invoked(file: File): Seq[Int] = {
    val reader = new BufferedReader(new FileReader(Env.measurementFile))
    val line = reader.readLine()
    reader.close()
    line.split(";").filterNot(_.isEmpty).map(_.toInt)
  }

  def serialize(coverage: Coverage, file: File) {
    val out = new FileOutputStream(file)
    out.write(serialize(coverage))
    out.close()
  }

  def serialize(coverage: Coverage): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(coverage)
    oos.close()
    baos.toByteArray
  }

  def deserialize(file: File): Coverage = deserialize(new FileInputStream(file))
  def deserialize(in: InputStream): Coverage = {
    val oos = new ObjectInputStream(in)
    val coverage = oos.readObject().asInstanceOf[Coverage]
    oos.close()
    coverage
  }
}
