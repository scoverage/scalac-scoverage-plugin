package scoverage

import java.io.FileWriter

/** @author Stephen Samuel */
object Invoker {

  def invoked(id: Int) = {
    val writer = new FileWriter(Env.measurementFile, true)
    writer.append(id.toString)
    writer.append(';')
    writer.close()
  }
}
