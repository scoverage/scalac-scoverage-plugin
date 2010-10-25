package reaktor.scct

import java.io._

object MetadataPickler {

  def toFile(data: List[CoveredBlock], f: File) {
    toOutputStream(data, new FileOutputStream(f))
  }

  private def toOutputStream(data: List[CoveredBlock], out: OutputStream) {
    IO.withOutputStream(new ObjectOutputStream(out)) { o =>
      data.foreach { o.writeObject _ }
    }
  }

  def load(f: File): List[CoveredBlock] = {
    fromInputStream(new FileInputStream(f))
  }

  private def fromInputStream(in: InputStream) = {
    IO.withInputStream(new ObjectInputStream(in)) { in => readObjects(in, Nil) }
  }

  private def readObjects(in: ObjectInputStream, acc:List[CoveredBlock]): List[CoveredBlock] = {
    readObj(in) match {
      case None => acc
      case Some(o) => readObjects(in, o :: acc)
    }
  }
  private def readObj(in: ObjectInputStream): Option[CoveredBlock] = {
    try {
      val obj = in.readObject.asInstanceOf[CoveredBlock]
      if (obj != null) Some(obj) else None
    } catch {
      case e: EOFException => None
    }
  }
}