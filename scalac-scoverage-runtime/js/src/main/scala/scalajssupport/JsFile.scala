package scalajssupport

trait JsFile {
  def delete(): Unit
  def getAbsolutePath(): String

  def getName(): String

  def getPath(): String

  def isDirectory(): Boolean

  def mkdirs(): Unit

  def listFiles(): Array[File]

  def listFiles(filter: FileFilter): Array[File] = {
    listFiles().filter(filter.accept)
  }

  def readFile(): String
}

trait FileFilter {
  def accept(file: File): Boolean
}

trait JsFileObject {
  def write(path: String, data: String, mode: String = "a")
  def pathJoin(path: String, child: String): String
  def apply(path: String): JsFile
}
