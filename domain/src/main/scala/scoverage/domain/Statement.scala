package scoverage.domain

import scala.collection.mutable

case class Statement(
    location: Location,
    id: Int,
    start: Int,
    end: Int,
    line: Int,
    desc: String,
    symbolName: String,
    treeName: String,
    branch: Boolean,
    var count: Int = 0,
    ignored: Boolean = false,
    tests: mutable.Set[String] = mutable.Set[String]()
) extends java.io.Serializable {
  def source = location.sourcePath
  def invoked(test: String): Unit = {
    count = count + 1
    if (test != "") tests += test
  }
  def isInvoked = count > 0
}
