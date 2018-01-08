package scalajssupport

import scala.scalajs.js

/**
 * This wraps RhinoFile, NodeFile, or PhantomFile depending on which javascript
 * environment is being used, and emulates a subset of the java.io.File API.
 */
class File(path: String) {
  import File._

  val _file = jsFile(path)

  def this(path: String, child: String) = {
    this(File.pathJoin(path, child))
  }

  def delete(): Unit = {
    _file.delete()
  }
  def getAbsolutePath(): String = {
    _file.getAbsolutePath()
  }

  def getName(): String = {
    _file.getName()
  }

  def getPath(): String = {
    _file.getPath()
  }

  def isDirectory(): Boolean = {
    _file.isDirectory()
  }

  def mkdirs(): Unit = {
    _file.mkdirs()
  }

  def listFiles(): Array[File] = {
    _file.listFiles().toArray
  }

  def listFiles(filter: FileFilter): Array[File] = {
    _file.listFiles().filter(filter.accept).toArray
  }

  def readFile(): String = {
    _file.readFile()
  }

  override def toString: String = {
    getPath()
  }
}

object File {
  val globalObject: js.Dynamic = {
    import js.Dynamic.{global => g}
    if (js.typeOf(g.global) != "undefined" && (g.global.Object eq g.Object)) {
      // Node.js environment detected
      g.global
    } else {
      // In all other well-known environment, we can use the global `this`
      js.special.fileLevelThis.asInstanceOf[js.Dynamic]
    }
  }

  val jsFile: JsFileObject = if (globalObject.hasOwnProperty("Packages").asInstanceOf[Boolean])
    RhinoFile
  else if (globalObject.hasOwnProperty("callPhantom").asInstanceOf[Boolean])
    PhantomFile
  else
    NodeFile
  // Factorize this

  def pathJoin(path: String, child: String): String =
    jsFile.pathJoin(path, child)

  def write(path: String, data: String, mode: String = "a") =
    jsFile.write(path, data, mode)
}