package scoverage

import java.io._

/** @author Stephen Samuel */
object IOUtils {

  // loads all the invoked statement ids
  def invoked(dir: File): Seq[Int] = {
    dir.listFiles.flatMap { file =>
      val reader = new BufferedReader(new FileReader(file))
      val line = reader.readLine()
      reader.close()
      line.split(";").filterNot(_.isEmpty).map(_.toInt)
    }
  }

  def serialize(coverage: Coverage, file: File) {
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
