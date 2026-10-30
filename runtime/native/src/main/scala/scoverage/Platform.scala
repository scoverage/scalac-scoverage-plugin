package scoverage

import java.io.{File => SupportFile}
import java.io.{FileFilter => SupportFileFilter}
import java.io.{FileWriter => SupportFileWriter}

import scala.collection.mutable.HashMap
import scala.io.{Source => SupportSource}
import scala.util.control.NonFatal

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
