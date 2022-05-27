package scalajssupport

import scala.scalajs.js

class NodeFile(path: String)(implicit fs: FS, nodePath: NodePath)
    extends JsFile {
  def this(path: String, child: String)(implicit fs: FS, nodePath: NodePath) = {
    this(nodePath.join(path, child))
  }

  def delete(): Unit = {
    if (isDirectory()) fs.rmdirSync(path)
    else fs.unlinkSync(path)
  }

  def getAbsolutePath(): String = {
    fs.realpathSync(path)
  }

  def getName(): String = {
    nodePath.basename(path)
  }

  def getPath(): String = {
    path
  }

  def isDirectory(): Boolean = {
    try {
      fs.lstatSync(path).isDirectory()
    } catch {
      // return false if the file does not exist
      case e: Exception => false
    }
  }

  def mkdirs(): Unit = {
    path
      .split("/")
      .foldLeft("")((acc: String, x: String) => {
        val new_acc = nodePath.join(acc, x)
        try {
          fs.mkdirSync(new_acc)
        } catch {
          case e: Exception =>
        }
        new_acc
      })
  }

  def listFiles(): Array[File] = {
    val files = fs.readdirSync(path)
    val filesArray = new Array[File](files.length)
    for ((item, i) <- filesArray.zipWithIndex) {
      filesArray(i) = new File(nodePath.join(this.getPath(), files(i)))
    }
    filesArray
  }

  def readFile(): String = {
    fs.readFileSync(path, js.Dynamic.literal(encoding = "utf-8"))
  }

}

@js.native
trait FSStats extends js.Object {
  def isDirectory(): Boolean = js.native
}

@js.native
trait FS extends js.Object {
  def closeSync(fd: Int): Unit = js.native
  def lstatSync(path: String): FSStats = js.native
  def mkdirSync(path: String): Unit = js.native
  def openSync(path: String, flags: String): Int = js.native
  def realpathSync(path: String): String = js.native
  def readdirSync(path: String): js.Array[String] = js.native
  def readFileSync(path: String, options: js.Dynamic): String = js.native
  def rmdirSync(path: String): Unit = js.native
  def unlinkSync(path: String): Unit = js.native
  def writeFileSync(
      path: String,
      data: String,
      options: js.Dynamic = js.Dynamic.literal()
  ): Unit = js.native
}

@js.native
trait NodePath extends js.Object {
  def basename(path: String): String = js.native
  def join(paths: String*): String = js.native
}

private[scalajssupport] trait NodeLikeFile extends JsFileObject {
  def require: js.Dynamic

  implicit lazy val fs: FS = require("fs").asInstanceOf[FS]
  implicit lazy val nodePath: NodePath = require("path").asInstanceOf[NodePath]

  def write(path: String, data: String, mode: String = "a") = {
    fs.writeFileSync(path, data, js.Dynamic.literal(flag = mode))
  }

  def pathJoin(path: String, child: String) = {
    nodePath.join(path, child)
  }

  def apply(path: String) = {
    new NodeFile(path)
  }
}

private[scalajssupport] object NodeFile extends NodeLikeFile {
  lazy val require = js.Dynamic.global.require
}

private[scalajssupport] object JSDOMFile extends NodeLikeFile {
  lazy val require = js.Dynamic.global.Node.constructor("return require")()
}
