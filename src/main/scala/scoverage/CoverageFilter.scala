package scoverage

/** @author Stephen Samuel */
class CoverageFilter(excludedPackages: Seq[String]) {
  def isIncluded(className: String): Boolean = {
    excludedPackages.isEmpty || !excludedPackages.exists(_.r.pattern.matcher(className).matches)
  }
}