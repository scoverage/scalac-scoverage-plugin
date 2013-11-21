package scales

/** @author Stephen Samuel */
sealed trait LineStatus
case object Covered extends LineStatus
case object MissingCoverage extends LineStatus
case object NotInstrumented extends LineStatus
