package reaktor.scct.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import reaktor.scct.report.MultiProjectHtmlReporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Learned so far:
 *   Everything in Maven keeps changing the destination directory, so we need both reportOutputDir and destDir to keep it in place.
 *   The aggregator and phase "annotation" here do pretty much nothing. And depending on the maven repoting configuration wrt v2 and v3, this plugin sometimes gets called for site and sometimes not.
 *
 * @goal scct-report
 * @phase site
 * @aggregator
 */
public class MultiProjectReportMojo extends AbstractMojo implements MavenReport {

  public MultiProjectReportMojo() {}

  public void execute() {
    List<File> files = collectCoverageFiles();
    scala.collection.immutable.List<File> scalaList = scala.collection.JavaConversions.asScalaBuffer(files).toList();
    MultiProjectHtmlReporter.report(scalaList, reportOutputDirectory);
  }

  private List<File> collectCoverageFiles() {
    List<MavenProject> modules = project.getCollectedProjects();
    List<File> files = new ArrayList<File>();
    for (MavenProject module : modules) {
      if ("pom".equalsIgnoreCase(module.getPackaging())) continue;
      File f = new File(module.getBuild().getDirectory() + "/coverage-report/coverage-result.data").getAbsoluteFile();
      if (f.exists()) {
        files.add(f);
      }
    }
    return files;
  }

  public void generate(@SuppressWarnings("unused") Sink sink, @SuppressWarnings("unused") Locale locale) throws MavenReportException {
    if (!canGenerateReport()) {
      getLog().warn("No child projects to aggregate report from. Sry.");
      return;
    }
    execute();
  }


  // Highly verbose configuration params:

  /**
   * Directory where reports will go.
   *
   * @parameter expression="${reportOutputDirectory}" default-value="${project.reporting.outputDirectory}/coverage-report"
   * @required
   */
  private File reportOutputDirectory;


  /**
   * Name of the last directory where reports go, under reportOutputDirectory.
   * @parameter expression="${destDir}" default-value="coverage-report"
   */
  private String destDir;

  /**
   * @parameter default-value="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  // The rest is boilerplate crap:

  public boolean isExternalReport() { return true; }
  public String getOutputName() { return reportOutputDirectory + "/index.html"; }
  public String getCategoryName() { return CATEGORY_PROJECT_REPORTS; }
  public String getName(Locale locale) { return "scct-report"; }
  public String getDescription(Locale locale)  { return "scct-report"; }
  public void setDestDir(String destDir) {
    this.destDir = destDir;
    setRealOutputDir(reportOutputDirectory, destDir);
  }
  public void setReportOutputDirectory(File file) {
    setRealOutputDir(file, destDir);
  }
  public File getReportOutputDirectory() {
    return reportOutputDirectory;
  }
  private void setRealOutputDir(File baseDir, String destDir) {
    if (baseDir != null && destDir != null && !baseDir.getAbsolutePath().endsWith(destDir)) {
      this.reportOutputDirectory = new File(baseDir, destDir);
    } else {
      this.reportOutputDirectory = baseDir;
    }
  }
  public boolean canGenerateReport() {
    return project.isExecutionRoot() && project.getCollectedProjects().size() > 0;
  }

}