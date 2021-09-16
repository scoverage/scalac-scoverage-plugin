package scoverage

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer

import scala.io.Codec
import scala.io.Source

object Serializer {

  // Write out coverage data to the given data directory, using the default coverage filename
  def serialize(coverage: Coverage, dataDir: String): Unit =
    serialize(coverage, coverageFile(dataDir))

  // Write out coverage data to given file.
  def serialize(coverage: Coverage, file: File): Unit = {
    val writer: Writer = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(file), Codec.UTF8.name)
    )
    try {
      serialize(coverage, writer)
    } finally {
      writer.flush()
      writer.close()
    }
  }

  def serialize(coverage: Coverage, writer: Writer): Unit = {
    def writeHeader(writer: Writer): Unit = {
      writer.write(s"""# Coverage data, format version: 2.0
                      |# Statement data:
                      |# - id
                      |# - source path
                      |# - package name
                      |# - class name
                      |# - class type (Class, Object or Trait)
                      |# - full class name
                      |# - method name
                      |# - start offset
                      |# - end offset
                      |# - line number
                      |# - symbol name
                      |# - tree name
                      |# - is branch
                      |# - invocations count
                      |# - is ignored
                      |# - description (can be multi-line)
                      |# '\f' sign
                      |# ------------------------------------------
                      |""".stripMargin.replaceAll("(\r\n)|\n|\r", "\n"))
    }

    def writeStatement(stmt: Statement, writer: Writer): Unit = {
      writer.write(s"""${stmt.id}
                      |${stmt.location.sourcePath}
                      |${stmt.location.packageName}
                      |${stmt.location.className}
                      |${stmt.location.classType}
                      |${stmt.location.fullClassName}
                      |${stmt.location.method}
                      |${stmt.start}
                      |${stmt.end}
                      |${stmt.line}
                      |${stmt.symbolName}
                      |${stmt.treeName}
                      |${stmt.branch}
                      |${stmt.count}
                      |${stmt.ignored}
                      |${stmt.desc}
                      |\f
                      |""".stripMargin.replaceAll("(\r\n)|\n|\r", "\n"))
    }

    writeHeader(writer)
    coverage.statements.toSeq
      .sortBy(_.id)
      .foreach(stmt => writeStatement(stmt, writer))
  }

  def coverageFile(dataDir: File): File = coverageFile(dataDir.getAbsolutePath)
  def coverageFile(dataDir: String): File =
    new File(dataDir, Constants.CoverageFileName)

  def deserialize(file: File): Coverage = {
    val source = Source.fromFile(file)(Codec.UTF8)
    try deserialize(source.getLines())
    finally source.close()
  }

  def deserialize(lines: Iterator[String]): Coverage = {
    def toStatement(lines: Iterator[String]): Statement = {
      val id: Int = lines.next().toInt
      val sourcePath = lines.next()
      val packageName = lines.next()
      val className = lines.next()
      val classType = lines.next()
      val fullClassName = lines.next()
      val method = lines.next()
      val loc = Location(
        packageName,
        className,
        fullClassName,
        ClassType.fromString(classType),
        method,
        sourcePath
      )
      val start: Int = lines.next().toInt
      val end: Int = lines.next().toInt
      val lineNo: Int = lines.next().toInt
      val symbolName: String = lines.next()
      val treeName: String = lines.next()
      val branch: Boolean = lines.next().toBoolean
      val count: Int = lines.next().toInt
      val ignored: Boolean = lines.next().toBoolean
      val desc = lines.toList.mkString("\n")
      Statement(
        loc,
        id,
        start,
        end,
        lineNo,
        desc,
        symbolName,
        treeName,
        branch,
        count,
        ignored
      )
    }

    val headerFirstLine = lines.next()
    require(
      headerFirstLine == "# Coverage data, format version: 2.0",
      "Wrong file format"
    )

    val linesWithoutHeader = lines.dropWhile(_.startsWith("#"))
    val coverage = Coverage()
    while (!linesWithoutHeader.isEmpty) {
      val oneStatementLines = linesWithoutHeader.takeWhile(_ != "\f")
      val statement = toStatement(oneStatementLines)
      if (statement.ignored)
        coverage.addIgnoredStatement(statement)
      else
        coverage.add(statement)
    }
    coverage
  }

}
