package scoverage

import scala.collection.mutable.HashMap

import scalajssupport.{File => SupportFile}
import scalajssupport.{FileFilter => SupportFileFilter}
import scalajssupport.{FileWriter => SupportFileWriter}
import scalajssupport.{Source => SupportSource}

object Platform {
  type ThreadSafeMap[A, B] = HashMap[A, B]
  lazy val ThreadSafeMap = HashMap

  type File = SupportFile
  type FileWriter = SupportFileWriter
  type FileFilter = SupportFileFilter

  lazy val Source = SupportSource

  def insecureRandomUUID() = {
    import scala.util.Random
    var msb = Random.nextLong()
    var lsb = Random.nextLong()
    msb &= 0xffffffffffff0fffL // clear version
    msb |= 0x0000000000004000L // set to version 4
    lsb &= 0x3fffffffffffffffL // clear variant
    lsb |= 0x8000000000000000L // set to IETF variant
    new java.util.UUID(msb, lsb)
  }

}
