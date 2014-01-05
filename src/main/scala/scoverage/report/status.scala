package scoverage.report

/** @author Stephen Samuel */
sealed trait StatementStatus
case object Invoked extends StatementStatus
case object NotInvoked extends StatementStatus
case object NoData extends StatementStatus
