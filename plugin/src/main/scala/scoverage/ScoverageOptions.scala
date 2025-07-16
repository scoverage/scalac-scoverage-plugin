package scoverage

/** Base options that can be passed into scoverage
  *
  * @param excludedPackages packages to be excluded in coverage
  * @param excludedFiles files to be excluded in coverage
  * @param excludedSymbols symbols to be excluded in coverage
  * @param dataDir the directory that the coverage files should be written to
  * @param reportTestName whether or not the test names should be reported
  * @param sourceRoot the source root of your project
  */
case class ScoverageOptions(
    excludedPackages: Seq[String],
    excludedFiles: Seq[String],
    excludedSymbols: Seq[String],
    dataDir: String,
    reportTestName: Boolean,
    sourceRoot: String
)

object ScoverageOptions {

  private[scoverage] val help = Some(
    Seq(
      "-P:scoverage:dataDir:<pathtodatadir>                  where the coverage files should be written\n",
      "-P:scoverage:sourceRoot:<pathtosourceRoot>            the root dir of your sources, used for path relativization\n",
      "-P:scoverage:excludedPackages:<regex>;<regex>         semicolon separated list of regexs for packages to exclude",
      "-P:scoverage:excludedFiles:<regex>;<regex>            semicolon separated list of regexs for paths to exclude",
      "-P:scoverage:excludedSymbols:<regex>;<regex>          semicolon separated list of regexs for symbols to exclude",
      "-P:scoverage:extraAfterPhase:<phaseName>;<phaseName>  phase after which scoverage phase runs (must be after typer phase)",
      "-P:scoverage:extraBeforePhase:<phaseName>;<phaseName> phase before which scoverage phase runs (must be before patmat phase)",
      "                                                      Any classes whose fully qualified name matches the regex will",
      "                                                      be excluded from coverage."
    ).mkString("\n")
  )

  private def parseExclusionOption(
      inOption: String
  ): Seq[String] =
    inOption
      .split(";")
      .collect {
        case value if value.trim().nonEmpty => value.trim()
      }
      .toIndexedSeq

  private val ExcludedPackages = "excludedPackages:(.*)".r
  private val ExcludedFiles = "excludedFiles:(.*)".r
  private val ExcludedSymbols = "excludedSymbols:(.*)".r
  private val DataDir = "dataDir:(.*)".r
  private val SourceRoot = "sourceRoot:(.*)".r
  private val ExtraAfterPhase = "extraAfterPhase:(.*)".r
  private val ExtraBeforePhase = "extraBeforePhase:(.*)".r

  /** Default that is _only_ used for initializing purposes. dataDir and
    * sourceRoot are both just empty strings here, but we nevery actually
    * allow for this to be the case when the plugin runs, and this is checked
    * before it does.
    */
  def default() = ScoverageOptions(
    excludedPackages = Seq.empty,
    excludedFiles = Seq.empty,
    excludedSymbols = Seq(
      "scala.reflect.api.Exprs.Expr",
      "scala.reflect.api.Trees.Tree",
      "scala.reflect.macros.Universe.Tree"
    ),
    dataDir = "",
    reportTestName = false,
    sourceRoot = ""
  )

  def processPhaseOptions(
      opts: List[String]
  ): (Option[List[String]], Option[List[String]]) = {

    val afterPhase: Option[List[String]] =
      opts.collectFirst { case ExtraAfterPhase(phase) =>
        phase.split(";").toList
      }
    val beforePhase: Option[List[String]] =
      opts.collectFirst { case ExtraBeforePhase(phase) =>
        phase.split(";").toList
      }

    (afterPhase, beforePhase)
  }

  def parse(
      scalacOptions: List[String],
      errFn: String => Unit,
      base: ScoverageOptions
  ): ScoverageOptions = {

    var options = base

    scalacOptions.foreach {
      case ExcludedPackages(packages) =>
        options =
          options.copy(excludedPackages = parseExclusionOption(packages))
      case ExcludedFiles(files) =>
        options = options.copy(excludedFiles = parseExclusionOption(files))
      case ExcludedSymbols(symbols) =>
        options = options.copy(excludedSymbols = parseExclusionOption(symbols))
      case DataDir(dir) =>
        options = options.copy(dataDir = dir)
      case SourceRoot(root) => options = options.copy(sourceRoot = root)
      // NOTE that both the extra phases are actually parsed out early on, so
      // we just ignore them here
      case ExtraAfterPhase(afterPhase)   => ()
      case ExtraBeforePhase(beforePhase) => ()
      case "reportTestName" =>
        options = options.copy(reportTestName = true)
      case opt => errFn("Unknown option: " + opt)
    }

    options
  }

}
