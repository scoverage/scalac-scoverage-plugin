package scoverage.reporter

trait MethodBuilders {
  def statements: Iterable[Statement]
  def methods: Seq[MeasuredMethod] = {
    statements
      .groupBy(stmt =>
        stmt.location.packageName + "/" + stmt.location.className + "/" + stmt.location.method
      )
      .map(arg => MeasuredMethod(arg._1, arg._2))
      .toSeq
  }
  def methodCount = methods.size
}

trait PackageBuilders {
  def statements: Iterable[Statement]
  def packageCount = packages.size
  def packages: Seq[MeasuredPackage] = {
    statements
      .groupBy(_.location.packageName)
      .map(arg => MeasuredPackage(arg._1, arg._2))
      .toSeq
      .sortBy(_.name)
  }
}
