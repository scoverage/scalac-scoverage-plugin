package scoverage

import scala.collection.concurrent.TrieMap
import java.io.{
  File => SupportFile,
  FileWriter => SupportFileWriter,
  FileFilter => SupportFileFilter
}
import scala.io.{ Source => SupportSource }

object Platform {
  type ThreadSafeMap[A, B] = TrieMap[A, B]
  lazy val ThreadSafeMap = TrieMap

  type File = SupportFile
  type FileWriter = SupportFileWriter
  type FileFilter = SupportFileFilter

  lazy val Source = SupportSource
}