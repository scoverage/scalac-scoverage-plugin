package reaktor.scct.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
 * @goal scct-report
 * @aggregator
 * @execute phase="generate-sources"
 */
public class MultiProjectReportMojo extends AbstractMojo implements MavenReport {

  public MultiProjectReportMojo() {
    System.out.println("MultiProjectReportMojo()");
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    System.out.println("MultiProjectReportMojo:EXECUTE!");
    List<File> files = collectCoverageFiles();
    for (File f : files) {
      System.out.println("Coverage: "+f.getAbsolutePath());
    }
    scala.collection.immutable.List<File> scalaList = scala.collection.JavaConversions.asScalaBuffer(files).toList();
    MultiProjectHtmlReporter.report(scalaList);
  }

  private List<File> collectCoverageFiles() {
    List<MavenProject> modules = project.getCollectedProjects();
    List<File> files = new ArrayList<File>();
    for (MavenProject module : modules) {
      if ("pom".equalsIgnoreCase(module.getPackaging())) continue;
      // TODO: parametrize file location
      File f = new File(module.getBuild().getDirectory() + "/coverage-report/coverage-result.data").getAbsoluteFile();
      System.out.println("Haz: " + f.getAbsolutePath()+" "+f.exists());
      if (f.exists()) {
        files.add(f);
      }
    }
    return files;
  }

  public void generate(@SuppressWarnings("unused") Sink sink, @SuppressWarnings("unused") Locale locale) throws MavenReportException {
    System.out.println("MultiProjectReportMojo:BUYACACHA!");
    if (!canGenerateReport()) {
      getLog().warn("No child projects to aggregate report from. Sry.");
      return;
    }
  }


  // Highly verbose configuration params:

  /**
   * Directory where reports will go.
   *
   * @parameter expression="${project.reporting.outputDirectory}/coverage-report"
   * @required
   */
  private File outputDirectory;

  /**
   * @parameter default-value="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  // The rest is boilerplate crap:

  public boolean isExternalReport() { return true; }
  public String getOutputName() { return outputDirectory + "/index.html"; }
  public String getCategoryName() { return CATEGORY_PROJECT_REPORTS; }
  public String getName(Locale locale) { return "scct-report"; }
  public String getDescription(Locale locale)  { return "scct-report"; }
  public void setReportOutputDirectory(File file) { outputDirectory = file; }
  public File getReportOutputDirectory() { return outputDirectory; }
  public boolean canGenerateReport() {
    System.out.println("MultiProjectReportMojo:canGenerateReport() "+project.isExecutionRoot() + " " + (project.getCollectedProjects().size() > 0));
    return project.isExecutionRoot() && project.getCollectedProjects().size() > 0;
  }

}