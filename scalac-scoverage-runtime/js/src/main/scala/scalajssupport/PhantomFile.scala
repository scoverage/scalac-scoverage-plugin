package scalajssupport

import scala.scalajs.js
import scala.scalajs.js.JSON

class PhantomFile(path: String) extends JsFile {
  def this(path: String, child: String) = {
    this(PhantomFile.pathJoin(path, child))
  }

  def delete(): Unit = {
    if (isDirectory()) PhantomFile.removeDirectory(path)
    else PhantomFile.remove(path)
  }

  def getAbsolutePath(): String = {
    PhantomFile.absolute(path)
  }

  def getName(): String = {
    path.split("\\" + PhantomFile.separator).last
  }

  def getPath(): String = {
    path
  }

  def isDirectory(): Boolean = {
    PhantomFile.isDirectory(path)
  }

  def mkdirs(): Unit = {
    PhantomFile.makeTree(path)
  }

  def listFiles(): Array[File] = {
    val files = PhantomFile.list(path)
    val filesArray = new Array[File](files.length)
    for ((item, i) <- filesArray.zipWithIndex) {
      filesArray(i) = new File(PhantomFile.pathJoin(this.getPath(), files(i)))
    }
    filesArray
  }

  def readFile(): String = {
    PhantomFile.read(path)
  }

}

private[scalajssupport] object PhantomFile extends JsFileObject {
  def fsCallArray(method: String, args: js.Array[js.Any]): js.Dynamic = {
    val d = js.Dynamic.global.callPhantom(js.Dynamic.literal(
      action = "require.fs",
      method = method,
      args = args
    ))
    JSON.parse(d.asInstanceOf[String])
  }

  def fsCall(method: String, arg: js.Any = null): js.Dynamic = {
    fsCallArray(method, js.Array(arg))

  }

  def absolute(path: String): String = fsCall("absolute", path).asInstanceOf[String]
  def isDirectory(path: String): Boolean = fsCall("isDirectory", path).asInstanceOf[Boolean]
  def list(path: String): js.Array[String] = fsCall("list", path).asInstanceOf[js.Array[String]]
  def makeTree(path: String): Boolean = fsCall("makeTree", path).asInstanceOf[Boolean]
  def read(path: String): String = fsCall("read", path).asInstanceOf[String]
  def remove(path: String): Boolean = fsCall("remove", path).asInstanceOf[Boolean]
  def removeDirectory(path: String): Boolean = fsCall("removeDirectory", path).asInstanceOf[Boolean]
  val separator: String = fsCall("separator").asInstanceOf[String]
  def write(path: String, content: String, mode: String): Unit = fsCallArray("write", js.Array(path, content, mode))
  def pathJoin(path: String, child: String): String = {
    return path + separator + child
  }

  def apply(path: String) = {
    new PhantomFile(path)
  }
}