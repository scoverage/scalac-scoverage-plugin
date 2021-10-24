package scoverage.domain

/** @param packageName the name of the enclosing package
  * @param className the name of the closest enclosing class
  * @param fullClassName the fully qualified name of the closest enclosing class
  */
case class Location(
    packageName: String,
    className: String,
    fullClassName: String,
    classType: ClassType,
    method: String,
    sourcePath: String
)
