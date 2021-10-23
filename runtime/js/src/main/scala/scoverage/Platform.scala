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

}
