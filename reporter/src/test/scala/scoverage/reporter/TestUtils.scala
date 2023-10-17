package scoverage.reporter

import scoverage.domain.Location
import scoverage.domain.ClassType
import scoverage.domain.Statement

object TestUtils {
  private var nextId = 0

  def testLocation(
      sourcePath: String,
      className: String = s"T$nextId",
      classType: ClassType = ClassType.Class
  ): Location =
    Location(
      "scoverage.test",
      className,
      s"scoverage.test.$className",
      classType,
      "method",
      sourcePath
    )

  def testStatement(
      location: Location,
      isBranch: Boolean = false,
      invokeCount: Int = 0
  ): Statement = {
    nextId += 1
    Statement(
      location,
      nextId,
      10 + nextId,
      50 + nextId,
      nextId * 10,
      nextId.toString,
      "sym",
      "",
      isBranch,
      invokeCount
    )
  }
}
