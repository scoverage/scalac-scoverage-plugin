package reaktor.scct.report

import reaktor.scct.{CoveredBlock, Env}
import xml.{PrettyPrinter, NodeSeq}

object CoberturaReporter {
  def report(blocks: List[CoveredBlock], env: Env) {
    new CoberturaReporter(new CoverageData(blocks), new HtmlReportWriter(env.reportDir), env).report
  }
}

class CoberturaReporter(data: CoverageData, writer: HtmlReportWriter, env: Env) {
  def report {
    val xml = <coverage line-rate={data.rate.getOrElse(0).toString}>
      <packages>
        {packages}
      </packages>
    </coverage>
    writer.write("cobertura.xml", new PrettyPrinter(120, 2).format(xml))
  }

  def packages = {
    for ((pkg, packageData) <- data.forPackages) yield {
      <package line-rate={packageData.rate.getOrElse(0).toString} name={pkg}>
        <classes>
          {classes(packageData)}
        </classes>
      </package>
    }
  }

  def classes(packageData: CoverageData) = {
    for ((clazz, classData) <- packageData.forClasses) yield
      <class line-rate={classData.rate.getOrElse(0).toString} name={clazz.className} filename={clazz.sourceFile}>
          <methods/>
        <lines>
          {lines(clazz.sourceFile, classData)}
        </lines>
      </class>
  }

  def lines(sourceFile: String, classData: CoverageData) = {
    val sourceLines = new SourceLoader().linesFor(sourceFile)
    line(1, 0, sourceLines, classData.blocks, NodeSeq.Empty)
  }

  def line(num: Int, offset: Int, sourceLines: List[String], blocks: List[CoveredBlock], acc: NodeSeq): NodeSeq =
    sourceLines match {
      case Nil => acc
      case sourceLine :: tail => {
        val maxOffset = offset + sourceLine.length
        val (currBlocks, nextBlocks) = blocks.partition(_.offset < maxOffset)
        val lineBlocks = currBlocks.filter(!_.placeHolder)
        var xml = acc
        if (lineBlocks.size != 0) {
          val hits = lineBlocks.filter(_.count > 0).size
          xml = xml ++ <line number={num.toString} hits={hits.toString}/>
        }
        line(num + 1, maxOffset, tail, nextBlocks, xml)
      }
    }
}
