package scoverage

import java.io.{File => SupportFile}
import java.io.{FileFilter => SupportFileFilter}
import java.io.{FileWriter => SupportFileWriter}

import scala.collection.concurrent.TrieMap
import scala.io.{Source => SupportSource}
import scala.util.control.NonFatal

object Platform {
  type ThreadSafeMap[A, B] = TrieMap[A, B]
  lazy val ThreadSafeMap = TrieMap

  type File = SupportFile
  type FileWriter = SupportFileWriter
  type FileFilter = SupportFileFilter

  lazy val Source = SupportSource

  def insecureRandomUUID() = java.util.UUID.randomUUID()

  def onShutdown(hook: () => Unit): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit =
        try hook()
        catch { case NonFatal(_) => () }
    })
    thread.setName("scoverage-measurement-dump")
    try Runtime.getRuntime.addShutdownHook(thread)
    catch {
      case _: IllegalStateException =>
        try hook()
        catch { case NonFatal(_) => () }
      case _: SecurityException =>
        ()
    }
  }

}
