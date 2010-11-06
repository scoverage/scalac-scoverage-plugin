package reaktor.scct

import io.Source
import java.io._

object IO extends IO

trait IO {
  val workingDir = new File(System.getProperty("user.dir"))
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

  def write(file: File, content: Array[Byte]) {
    withOutputStream(new FileOutputStream(file)) { out => out.write(content) }
  }

  def write(fileName: String, content: String) {
    write(new File(fileName), content.getBytes("utf-8"))
  }

  def write(file: File, content: String) {
    write(file, content.getBytes("utf-8"))
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

  def relativePath(f: File): String = relativePath(f, workingDir)
  def relativePath(f: File, to: File): String = to.toURI.relativize(f.toURI).toString

}