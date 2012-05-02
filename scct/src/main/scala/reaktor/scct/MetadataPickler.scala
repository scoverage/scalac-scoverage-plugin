package reaktor.scct

import java.io._
import annotation.tailrec

object MetadataPickler {

  def toFile(data: List[CoveredBlock], f: File) {
    IO.writeObjects(f) { out => data.foreach { out.writeObject _ } }
  }

  def load(f: File): List[CoveredBlock] = {
    IO.readObjects(f) { readObjects(_, Nil) }
  }

  @tailrec private def readObjects(in: ObjectInputStream, acc:List[CoveredBlock]): List[CoveredBlock] = {
    readObj(in) match {
      case None => acc
      case Some(o) => readObjects(in, o :: acc)
    }
  }
  def readObj(in: ObjectInputStream): Option[CoveredBlock] = {
    try {
      val obj = in.readObject.asInstanceOf[CoveredBlock]
      if (obj != null) Some(obj) else None
    } catch {
      case e: EOFException => None
    }
  }
}