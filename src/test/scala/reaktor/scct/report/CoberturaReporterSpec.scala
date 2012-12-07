package reaktor.scct.report

import org.specs2.mutable._
import java.io.File
import reaktor.scct.{IO, ClassTypes, Name, CoveredBlock}
import xml.XML
import org.specs2.specification.Scope

class CoberturaReporterSpec extends Specification {

  sequential

  val tmpDir = new File(System.getProperty("java.io.tmpdir", "/tmp"))
  val sourceFile = new File(tmpDir, "CoberturaReportSpec.scala")
  val outputFile = new File(tmpDir, "cobertura.xml")

  val name = Name(sourceFile.getName, ClassTypes.Class, "reaktor.scct.report", "CoberturaReportSpec", "scct")

  "report output" in new CleanEnv {
    IO.write(sourceFile, 1.to(4).map((ii:Int) => "0123456789").mkString("\n").getBytes("utf-8"))
    val blocks = List(
      new CoveredBlock("c1", 0, name, 0, false).increment,
      new CoveredBlock("c1", 1, name, 11, false),
      new CoveredBlock("c1", 1, name, 23, false).increment,
      new CoveredBlock("c1", 2, name, 28, false).increment
    )
    val projectData = ProjectData("myProject", tmpDir, tmpDir, blocks.toArray)
    val sut = new CoberturaReporter(projectData, new HtmlReportWriter(tmpDir))
    sut.report
    XML.loadFile(outputFile) must beEqualToIgnoringSpace(
      <coverage line-rate="0.75">
        <packages>
          <package line-rate="0.75" name="reaktor.scct.report">
            <classes>
              <class line-rate="0.75" name="CoberturaReportSpec" filename="CoberturaReportSpec.scala">
                <methods></methods>
                <lines>
                  <line hits="1" number="1"></line>
                  <line hits="0" number="2"></line>
                  <line hits="2" number="3"></line>
                </lines>
              </class>
            </classes>
          </package>
        </packages>
      </coverage>
    )
  }

  "tail recursive source line reading" in new CleanEnv {
    IO.write(sourceFile, 1.to(4000).mkString("\n").getBytes("utf-8"))

    val projectData = ProjectData("myProject", tmpDir, tmpDir, blocks(4000, name).toArray)
    val sut = new CoberturaReporter(projectData, new HtmlReportWriter(tmpDir))
    sut.report
    outputFile must beAFile
  }

  trait CleanEnv extends Scope {
    if (sourceFile.exists()) sourceFile.delete()
    if (outputFile.exists()) outputFile.delete()
  }

  def blocks(ii: Int, name: Name) = {
    1.to(ii).map { ii:Int =>
      val b = new CoveredBlock("c1", ii, name, ii, false)
      if (ii % 2 == 0) b.increment
      b
    }.toList
  }
}