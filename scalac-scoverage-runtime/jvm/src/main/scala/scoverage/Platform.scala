package scoverage

import java.io.{File => SupportFile}
import java.io.{FileFilter => SupportFileFilter}
import java.io.{FileWriter => SupportFileWriter}

import scala.collection.concurrent.TrieMap
import scala.io.{Source => SupportSource}

object Platform {
  type ThreadSafeMap[A, B] = TrieMap[A, B]
  lazy val ThreadSafeMap = TrieMap

  type File = SupportFile
  type FileWriter = SupportFileWriter
  type FileFilter = SupportFileFilter

  lazy val Source = SupportSource
}
