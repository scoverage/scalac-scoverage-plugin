package scoverage

import java.io.File
import java.util.UUID

import org.scalatest.{FreeSpec, Matchers}
import scoverage.report.{ScoverageXmlReader, ScoverageXmlWriter}

class ScoverageXmlReaderTest extends FreeSpec with Matchers {

  "scoverage xml reader" - {
    "should read output from ScoverageXmlWriter" in {

      val coverage = Coverage()

      coverage.add(Statement("/home/sam/src/main/scala/com/scoverage/class.scala",
        Location("com.scoverage",
          "Test",
          "TopLevel",
          ClassType.Object,
          "somemeth",
          "/home/sam/src/main/scala/com/scoverage/class.scala"),
        14,
        155,
        176,
        4,
        "",
        "",
        "",
        true,
        2))

      coverage.add(Statement("/home/sam/src/main/scala/com/scoverage/foo/class.scala",
        Location("com.scoverage.foo",
          "ServiceState",
          "Service",
          ClassType.Trait,
          "methlab",
          "/home/sam/src/main/scala/com/scoverage/foo/class.scala"),
        16,
        95,
        105,
        19,
        "",
        "",
        "",
        false,
        0))

      val temp = new File(IOUtils.getTempPath, UUID.randomUUID.toString)
      temp.mkdir()
      temp.deleteOnExit()
      new ScoverageXmlWriter(new File("/home/sam"), temp, false).write(coverage)

      val actual = ScoverageXmlReader.read(IOUtils.reportFile(temp, false))
      // we don't care about the statement ids as the will change on reading back in
      actual.statements.map(_.copy(id = 0)).toSet shouldEqual coverage.statements.map(_.copy(id = 0)).toSet

    }
  }
}
