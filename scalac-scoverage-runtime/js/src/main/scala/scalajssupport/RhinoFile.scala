package scalajssupport

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

import js.Dynamic.{ global => g, newInstance => jsnew }

@JSName("Packages.java.io.File")
class NativeRhinoFile(path: String, child: String) extends js.Object {
  def this(path: String) = this("", path)

  def delete(): Unit = js.native

  def getAbsolutePath(): Any = js.native

  def getName(): Any = js.native

  def getPath(): Any = js.native

  def isDirectory(): Boolean = js.native

  def length(): js.Any = js.native

  def mkdirs(): Unit = js.native

  def listFiles(): js.Array[NativeRhinoFile] = js.native
}

class RhinoFile(_file: NativeRhinoFile) extends JsFile {
  def this(path: String) = this(new NativeRhinoFile(path))

  def this(path: String, child: String) = {
    this((new NativeRhinoFile(path, child)))
  }

  def delete(): Unit = _file.delete()

  def getAbsolutePath(): String = "" + _file.getAbsolutePath()

  def getName(): String = "" + _file.getName()

  def getPath(): String = {
    "" + _file.getPath() // Rhino bug: doesn't seem to actually returns a string, we have to convert it ourselves
  }

  def isDirectory(): Boolean = _file.isDirectory()

  def mkdirs(): Unit = _file.mkdirs()

  def listFiles(): Array[File] = {
    val files = _file.listFiles()
    val filesArray = new Array[File](files.length)
    for ((item, i) <- filesArray.zipWithIndex) {
      filesArray(i) = new File("" + files(i).getAbsolutePath())
    }
    filesArray
  }

  def readFile(): String = {
    val fis = jsnew(g.Packages.java.io.FileInputStream)(_file)
    val data = g.Packages.java.lang.reflect.Array.newInstance(
      g.Packages.java.lang.Byte.TYPE, _file.length()
    )
    fis.read(data)
    fis.close()
    "" + jsnew(g.Packages.java.lang.String)(data)
  }
}

private[scalajssupport] object RhinoFile extends JsFileObject {
  def write(path: String, data: String, mode: String) = {
    val outputstream = jsnew(g.Packages.java.io.FileOutputStream)(path, mode == "a")
    val jString = jsnew(g.Packages.java.lang.String)(data)
    outputstream.write(jString.getBytes())
  }

  def pathJoin(path: String, child: String) = {
    "" + (new NativeRhinoFile(path, child)).getPath()
  }

  def apply(path: String) = {
    new RhinoFile(path)
  }
}