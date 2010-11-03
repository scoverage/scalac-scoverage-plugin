import java.io._
import scala.xml._

object GenerateIndeces extends Application {
  val root = new File("maven-repo")
  recurse(root)

  def recurse(file: File): Unit = {
    if (file.isDirectory) {
      write(file, generateIndex(file))
    }
    file.listFiles().filter(_.isDirectory()).foreach { f =>
      recurse(f)
    }
  }

  def generateIndex(dir: File) = {
    val dirs = dir.listFiles.filter(_.isDirectory).map(_.getName + "/").toList.sorted
    val files = dir.listFiles.filter(f => f.isFile && f.getName != "index.html").map(_.getName).toList.sorted
    <html>
    <head><link type="text/css" href={rootLink(dir) + "/maven-repo.css"} rel="stylesheet" /></head>
    <body>
      <h1>{ path(dir) }</h1>
      <ul>
        <li><a href="../">..</a></li>
        { for (f <- dirs ::: files) yield <li><a href={ f }>{ f }</a></li> }
      </ul>
    </body>
    </html>
  }

  def path(dir: File): String = if (dir == root) "scct/maven-repo" else path(dir.getParentFile) + "/" + dir.getName

  def rootLink(dir: File): String = {
    if (dir == root) ".." else rootLink(dir.getParentFile) + "/.."
  }

  def write(dir: File, n: Node) {
    val out = new FileWriter(new File(dir, "index.html"))
    out.write("""<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">""")
    out.write(new PrettyPrinter(120,2).format(n))
    out.close
  }
}

