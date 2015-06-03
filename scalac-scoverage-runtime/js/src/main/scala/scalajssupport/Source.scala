package scalajssupport

import scala.io.{ Source => OrigSource }

/**
* This implementation of Source loads the whole file in memory, which is not really efficient, but
* it is not a problem for scoverage operations.
*/  
object Source {
  def fromFile(file: File) = {
    new OrigSource {

      val iter = file.readFile.toCharArray.iterator
    }
  }
}