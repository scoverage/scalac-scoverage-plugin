package reaktor.scct

import io.Source
import java.io._

object IO extends IO

trait IO {
  def withInputStream[A, B <: InputStream](in: B)(func: B => A): A = {
    try {
      func(in)
    } finally {
      in.close
    }
  }
  def withOutputStream[A, B <: OutputStream](out: B)(func: B => A): A = {
    try {
      func(out)
    } finally {
      out.close
    }
  }

  def read[T](file: File)(func: InputStream => T) = {
    withInputStream(new FileInputStream(file)) { in => func(in) }
  }

  def write(file: File)(func: OutputStream => Unit) {
    withOutputStream(new FileOutputStream(file)) { out => func(out) }
  }

  def readObjects[T](file:File)(func: ObjectInputStream => T) = {
    read(file) { in => func(new ClassLoaderedObjectInputStream(in, this.getClass.getClassLoader)) }
  }
  def writeObjects(file:File)(func: ObjectOutputStream => Unit) {
    write(file) { out => func(new ObjectOutputStream(out)) }
  }

  def write(file: File, content: Array[Byte]) {
    write(file) { _.write(content) }
  }

  def readResource(resourceName: String): String = {
    withInputStream(getClass.getResourceAsStream(resourceName)) { in =>
      Source.fromInputStream(in, "utf-8").mkString
    }
  }
  def readResourceBytes(resourceName: String): Array[Byte] = {
    withInputStream(getClass.getResourceAsStream(resourceName)) { in =>
      toBytes(in)
    }
  }

  private def toBytes(in: InputStream) = {
    val out = new ByteArrayOutputStream
    try {
      transfer(in, out, new Array[Byte](8192))
    } finally {
      out.close()
    }
    out.toByteArray()
  }

  private def transfer(in: InputStream, out: OutputStream, buffer: Array[Byte]): OutputStream = {
    val count = in.read(buffer)
    if (count > 0) {
      out.write(buffer, 0, count)
      transfer(in, out, buffer)
    } else {
      out
    }
  }

  def relativePath(f: File, to: File): String = to.toURI.relativize(f.toURI).toString


  class ClassLoaderedObjectInputStream(in:InputStream, val loader:ClassLoader) extends ObjectInputStream(in) {
    override def resolveClass(desc: ObjectStreamClass) = {
      Class.forName(desc.getName(), false, loader)
    }
  }
}