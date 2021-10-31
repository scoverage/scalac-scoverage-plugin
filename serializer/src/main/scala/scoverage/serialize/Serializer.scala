package scoverage.serialize

import java.io.BufferedWriter
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer

import scala.io.Codec
import scala.io.Source

import scoverage.domain.ClassType
import scoverage.domain.Constants
import scoverage.domain.Coverage
import scoverage.domain.Location
import scoverage.domain.Statement

object Serializer {

  def coverageFile(dataDir: File): File = coverageFile(dataDir.getAbsolutePath)
  def coverageFile(dataDir: String): File =
    new File(dataDir, Constants.CoverageFileName)

  // Write out coverage data to the given data directory, using the default coverage filename
  def serialize(coverage: Coverage, dataDir: String, sourceRoot: String): Unit =
    serialize(coverage, coverageFile(dataDir), new File(sourceRoot))

  // Write out coverage data to given file.
  def serialize(coverage: Coverage, file: File, sourceRoot: File): Unit = {
    val writer: Writer = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(file), Codec.UTF8.name)
    )
    try {
      serialize(coverage, writer, sourceRoot)
    } finally {
      writer.flush()
      writer.close()
    }
  }

  def serialize(
      coverage: Coverage,
      writer: Writer,
      sourceRoot: File
  ): Unit = {
    def getRelativePath(filePath: String): String = {
      val base = sourceRoot.getCanonicalFile().toPath()
      // NOTE: In the real world I have no idea if it's likely that you'll end
      // up with weird issues on windows where the roots don't match, something
      // like your root being D:/ and your file being C:/. If so this blows up.
      // This happened on windows CI for me, since I was using a temp dir, and
      // then trying to reletavize it off the cwd, which were in different
      // drives. For now, we'll let this as is, but if 'other' has different
      // root ever shows its, we'll shut that down real quick here... just not
      // sure what to do in that situation yet.
      val relPath =
        base.relativize(new File(filePath).getCanonicalFile().toPath())
      relPath.toString
    }

    def writeHeader(writer: Writer): Unit = {
      writer.write(
        s"""# Coverage data, format version: ${Constants.CoverageDataFormatVersion}
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
           |""".stripMargin
      )
    }

    def writeStatement(stmt: Statement, writer: Writer): Unit = {
      writer.write(s"""${stmt.id}
                      |${getRelativePath(stmt.location.sourcePath)}
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
                      |""".stripMargin)
    }

    writeHeader(writer)
    coverage.statements.toSeq
      .sortBy(_.id)
      .foreach(stmt => writeStatement(stmt, writer))
  }

  def deserialize(file: File, sourceRoot: File): Coverage = {
    val source = Source.fromFile(file)(Codec.UTF8)
    try deserialize(source.getLines(), sourceRoot)
    finally source.close()
  }

  def deserialize(lines: Iterator[String], sourceRoot: File): Coverage = {
    // To integrate it smoothly with rest of the report writers,
    // it is necessary to again convert [sourcePath] into a
    // canonical one.
    def getAbsolutePath(filePath: String): String = {
      new File(sourceRoot, filePath).getCanonicalPath()
    }

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
        getAbsolutePath(sourcePath)
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
      headerFirstLine == s"# Coverage data, format version: ${Constants.CoverageDataFormatVersion}",
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

  def clean(dataDir: File): Unit =
    findMeasurementFiles(dataDir).foreach(_.delete)
  def clean(dataDir: String): Unit = clean(new File(dataDir))

  def findMeasurementFiles(dataDir: File): Array[File] =
    dataDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean =
        pathname.getName.startsWith(Constants.MeasurementsPrefix)
    })

}
