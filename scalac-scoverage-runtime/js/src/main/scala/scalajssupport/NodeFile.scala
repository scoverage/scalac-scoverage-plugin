package scalajssupport

import scala.scalajs.js

class NodeFile(path: String) extends JsFile {
  def this(path: String, child: String) = {
    this(NodeFile.nodePath.join(path, child))
  }

  def delete(): Unit = {
    if (isDirectory()) NodeFile.fs.rmdirSync(path)
    else NodeFile.fs.unlinkSync(path)
  }

  def getAbsolutePath(): String = {
    NodeFile.fs.realpathSync(path)
  }

  def getName(): String = {
    NodeFile.nodePath.basename(path)
  }

  def getPath(): String = {
    path
  }

  def isDirectory(): Boolean = {
    try {
      NodeFile.fs.lstatSync(path).isDirectory()
    } catch {
      // return false if the file does not exist
      case e: Exception => false
    }
  }

  def mkdirs(): Unit = {
    path.split("/").foldLeft("")((acc: String, x: String) => {
      val new_acc = NodeFile.nodePath.join(acc, x)
      try {
        NodeFile.fs.mkdirSync(new_acc)
      } catch {
        case e: Exception =>
      }
      new_acc
    })
  }

  def listFiles(): Array[File] = {
    val files = NodeFile.fs.readdirSync(path)
    val filesArray = new Array[File](files.length)
    for ((item, i) <- filesArray.zipWithIndex) {
      filesArray(i) = new File(NodeFile.nodePath.join(this.getPath(), files(i)))
    }
    filesArray
  }

  def readFile(): String = {
    NodeFile.fs.readFileSync(path, js.Dynamic.literal(encoding = "utf-8"))
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
  def writeFileSync(path: String, data: String, options: js.Dynamic = js.Dynamic.literal()): Unit = js.native
}

@js.native
trait NodePath extends js.Object {
  def basename(path: String): String = js.native
  def join(paths: String*): String = js.native
}

private[scalajssupport] object NodeFile extends JsFileObject {
  val fs: FS = js.Dynamic.global.require("fs").asInstanceOf[FS]
  val nodePath: NodePath = js.Dynamic.global.require("path").asInstanceOf[NodePath]
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