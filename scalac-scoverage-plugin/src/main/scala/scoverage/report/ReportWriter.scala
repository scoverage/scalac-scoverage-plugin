package scoverage.report

import java.io.File

import org.rogach.scallop.ArgType.V
import org.rogach.scallop.{singleArgConverter, ValueConverter, Scallop}
import scoverage.{IOUtils, Serializer, Constants, Coverage}

import language.implicitConversions

object ReportWriter {

  def writeReports(outputDirectory: File,
                   baseDirectory: File,
                   coverage: Coverage,
                   coverageOutputCobertura: Boolean,
                   coverageOutputXML: Boolean,
                   coverageOutputHTML: Boolean,
                   coverageDebug: Boolean): Unit = {

    val reportDir = new File(outputDirectory, "scoverage-report")
    reportDir.mkdirs()

    if (coverageOutputCobertura) {
      val coberturaDir = new File(outputDirectory, "coverage-report")
      coberturaDir.mkdirs()
      new CoberturaXmlWriter(baseDirectory, coberturaDir).write(coverage)
    }

    if (coverageOutputXML) {
      new ScoverageXmlWriter(baseDirectory, reportDir, false).write(coverage)
      if (coverageDebug) {
        new ScoverageXmlWriter(baseDirectory, reportDir, true).write(coverage)
      }
    }

    if (coverageOutputHTML) {
      new ScoverageHtmlWriter(baseDirectory, reportDir).write(coverage)
    }

  }
}

object Reports extends App {

  private val defaultsToTrue = () => Some(true)
  private implicit val stringToFile = singleArgConverter(new File(_))

  val BASE_DIR: String = "base-dir"
  val OUTPUT_DIR: String = "output-dir"
  val AGGREGATE_SUB_PROJECTS: String = "aggregate"
  val CLEAN_AGGREGATED_FILES: String = "clean"
  val SINGLE_DATA_DIR: String = "data-dir"
  val OUTPUT_COBERTURA_XML: String = "cobertura"
  val OUTPUT_SCOVERAGE_DEBUG_XML: String = "debug"
  val OUTPUT_SCOVERAGE_XML: String = "xml"
  val OUTPUT_SCOVERAGE_HTML: String = "html"

  private val opts = Scallop(args)
    .opt[File](BASE_DIR, required = true)
    .opt[File](OUTPUT_DIR, required = true)
    .opt[Boolean](AGGREGATE_SUB_PROJECTS, required = false, default = () => None)
    .opt[File](SINGLE_DATA_DIR, required = false, validate = f => Serializer.coverageFile(f).exists())
    .opt[Boolean](CLEAN_AGGREGATED_FILES, default = defaultsToTrue)
    .opt[Boolean](OUTPUT_SCOVERAGE_DEBUG_XML, default = defaultsToTrue)
    .opt[Boolean](OUTPUT_COBERTURA_XML, default = defaultsToTrue)
    .opt[Boolean](OUTPUT_SCOVERAGE_XML, default = defaultsToTrue)
    .opt[Boolean](OUTPUT_SCOVERAGE_HTML, default = defaultsToTrue)
    .verify

  private val baseDir = opts[File](BASE_DIR)

  private val coverage: Option[Coverage] = {
    // opts.get[_] appears not to work as expected here
    (opts.isSupplied(SINGLE_DATA_DIR), opts.isSupplied(AGGREGATE_SUB_PROJECTS)) match {
      case (true, false) =>
        val dataDir = opts[File](SINGLE_DATA_DIR)
        val coverageFile = Serializer.coverageFile(dataDir)
        if (coverageFile.exists()) {
          val coverage = Serializer.deserialize(coverageFile)
          val measurementFiles = IOUtils.findMeasurementFiles(dataDir)
          val measurements = IOUtils.invoked(measurementFiles)
          coverage.apply(measurements)
          Some(coverage)
        } else {
          None
        }
      case (false, true) =>
        CoverageAggregator.aggregate(baseDir, opts[Boolean](CLEAN_AGGREGATED_FILES))
      case (true, true)=>
        Console.err.println(s"Cannot specify $SINGLE_DATA_DIR and $AGGREGATE_SUB_PROJECTS")
        None
      case (false, false) =>
        Console.err.println(s"Must specify $SINGLE_DATA_DIR or $AGGREGATE_SUB_PROJECTS")
        None
    }
  }

  coverage match {
    case Some(data) =>
      ReportWriter.writeReports(
        opts[File](OUTPUT_DIR),
        baseDir,
        data,
        opts[Boolean](OUTPUT_COBERTURA_XML),
        opts[Boolean](OUTPUT_SCOVERAGE_XML),
        opts[Boolean](OUTPUT_SCOVERAGE_HTML),
        opts[Boolean](OUTPUT_SCOVERAGE_DEBUG_XML))
      println("Wrote reports")
    case None =>
      Console.err.println(s"No coverage found")
      sys.exit(1)
  }

}

