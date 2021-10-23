package scoverage.reporter

import java.io.File
import java.io.FileWriter
import java.util.UUID

import munit.FunSuite

class CoverageAggregatorTest extends FunSuite {

  // Let current directory be our source root
  private val sourceRoot = new File(".").getCanonicalPath()
  private def canonicalPath(fileName: String) =
    new File(sourceRoot, fileName).getCanonicalPath()

  test("should merge coverage objects with same id") {

    val source = canonicalPath("com/scoverage/class.scala")
    val location = Location(
      "com.scoverage.foo",
      "ServiceState",
      "com.scoverage.foo.Service.ServiceState",
      ClassType.Trait,
      "methlab",
      source
    )

    val cov1Stmt1 = Statement(location, 1, 155, 176, 4, "", "", "", true, 1)
    val cov1Stmt2 = Statement(location, 2, 200, 300, 5, "", "", "", false, 1)
    val coverage1 = Coverage()
    coverage1.add(cov1Stmt1.copy(count = 0))
    coverage1.add(cov1Stmt2.copy(count = 0))
    val dir1 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
    dir1.mkdir()
    Serializer.serialize(
      coverage1,
      Serializer.coverageFile(dir1),
      new File(sourceRoot)
    )
    val measurementsFile1 =
      new File(dir1, s"${Constants.MeasurementsPrefix}1")
    val measurementsFile1Writer = new FileWriter(measurementsFile1)
    measurementsFile1Writer.write("1\n2\n")
    measurementsFile1Writer.close()

    val cov2Stmt1 = Statement(location, 1, 95, 105, 19, "", "", "", false, 0)
    val coverage2 = Coverage()
    coverage2.add(cov2Stmt1)
    val dir2 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
    dir2.mkdir()
    Serializer.serialize(
      coverage2,
      Serializer.coverageFile(dir2),
      new File(sourceRoot)
    )

    val cov3Stmt1 =
      Statement(location, 2, 14, 1515, 544, "", "", "", false, 1)
    val coverage3 = Coverage()
    coverage3.add(cov3Stmt1.copy(count = 0))
    val dir3 = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
    dir3.mkdir()
    Serializer.serialize(
      coverage3,
      Serializer.coverageFile(dir3),
      new File(sourceRoot)
    )
    val measurementsFile3 =
      new File(dir3, s"${Constants.MeasurementsPrefix}1")
    val measurementsFile3Writer = new FileWriter(measurementsFile3)
    measurementsFile3Writer.write("2\n")
    measurementsFile3Writer.close()

    val aggregated =
      CoverageAggregator.aggregatedCoverage(
        Seq(dir1, dir2, dir3),
        new File(sourceRoot)
      )
    assertEquals(aggregated.statements.toSet.size, 4)
    assertEquals(
      aggregated.statements.map(_.copy(id = 0)).toSet,
      Set(cov1Stmt1, cov1Stmt2, cov2Stmt1, cov3Stmt1).map(_.copy(id = 0))
    )
  }
}
