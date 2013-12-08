package scoverage

import java.io.FileWriter

/** @author Stephen Samuel */
object Invoker {

  def invoked(id: Int, path: String) = {
    val writer = new FileWriter(path, true)
    writer.append(id.toString)
    writer.append(';')
    writer.close()
  }
}
