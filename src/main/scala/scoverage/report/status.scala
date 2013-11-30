package scoverage.report

/** @author Stephen Samuel */
sealed trait StatementStatus
case object Covered extends StatementStatus
case object MissingCoverage extends StatementStatus
case object NotInstrumented extends StatementStatus
