package scoverage.reporter

import java.io.File

import scoverage.reporter.DoubleFormat.twoFractionDigits

trait CoverageMetrics {
  def statements: Iterable[Statement]
  def statementCount: Int = statements.size

  def ignoredStatements: Iterable[Statement]
  def ignoredStatementCount: Int = ignoredStatements.size

  def invokedStatements: Iterable[Statement] = statements.filter(_.count > 0)
  def invokedStatementCount = invokedStatements.size
  def statementCoverage: Double = if (statementCount == 0) 1
  else invokedStatementCount / statementCount.toDouble
  def statementCoveragePercent = statementCoverage * 100
  def statementCoverageFormatted: String = twoFractionDigits(
    statementCoveragePercent
  )
  def branches: Iterable[Statement] = statements.filter(_.branch)
  def branchCount: Int = branches.size
  def branchCoveragePercent = branchCoverage * 100
  def invokedBranches: Iterable[Statement] = branches.filter(_.count > 0)
  def invokedBranchesCount = invokedBranches.size

  /** @see http://stackoverflow.com/questions/25184716/scoverage-ambiguous-measurement-from-branch-coverage
    */
  def branchCoverage: Double = {
    // if there are zero branches, then we have a single line of execution.
    // in that case, if there is at least some coverage, we have covered the branch.
    // if there is no coverage then we have not covered the branch
    if (branchCount == 0) {
      if (statementCoverage > 0) 1
      else 0
    } else {
      invokedBranchesCount / branchCount.toDouble
    }
  }
  def branchCoverageFormatted: String = twoFractionDigits(branchCoveragePercent)
}

case class MeasuredMethod(name: String, statements: Iterable[Statement])
    extends CoverageMetrics {
  override def ignoredStatements: Iterable[Statement] = Seq()
}

case class MeasuredClass(fullClassName: String, statements: Iterable[Statement])
    extends CoverageMetrics
    with MethodBuilders {

  def source: String = statements.head.source
  def loc = statements.map(_.line).max

  /** The class name for display is the FQN minus the package,
    * for example "com.a.Foo.Bar.Baz" should display as "Foo.Bar.Baz"
    * and "com.a.Foo" should display as "Foo".
    *
    * This is used in the class lists in the package and overview pages.
    */
  def displayClassName = statements.headOption
    .map(_.location)
    .map { location =>
      location.fullClassName.stripPrefix(location.packageName + ".")
    }
    .getOrElse(fullClassName)

  override def ignoredStatements: Iterable[Statement] = Seq()
}

case class MeasuredPackage(name: String, statements: Iterable[Statement])
    extends CoverageMetrics
    with ClassCoverage
    with ClassBuilders
    with FileBuilders {
  override def ignoredStatements: Iterable[Statement] = Seq()
}

case class MeasuredFile(source: String, statements: Iterable[Statement])
    extends CoverageMetrics
    with ClassCoverage
    with ClassBuilders {
  def filename = new File(source).getName
  def loc = statements.map(_.line).max

  override def ignoredStatements: Iterable[Statement] = Seq()
}

trait ClassCoverage {
  this: ClassBuilders =>
  val statements: Iterable[Statement]
  def invokedClasses: Int = classes.count(_.statements.count(_.count > 0) > 0)
  def classCoverage: Double = invokedClasses / classes.size.toDouble
}
