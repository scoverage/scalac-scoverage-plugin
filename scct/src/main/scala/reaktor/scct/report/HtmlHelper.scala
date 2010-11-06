package reaktor.scct.report

import reaktor.scct.{ClassTypes, Name}
import xml.{Node, NodeSeq, Text}

class HtmlHelper {
  def packageReportFileName(name: String) = toFileName("pkg", name)
  def sourceReportFileName(name: String) = toFileName("src", name)

  private def toFileName(prefix: String, name: String) =
    prefix + "-" + name.replaceAll("/", "_").replaceAll("<", "_").replaceAll(">", "_") + ".html"

  def table(headerRow: NodeSeq, itemRows: NodeSeq) =
    <table><tbody>{ headerRow }{ itemRows }</tbody></table>

  def headerRow(name: String, percentage: Option[Int]) =
    <tr>
      <td class="barContainerLeft header">{ name }</td>
      <td class="barContainerRight">{ percentageBar(percentage) }</td>
    </tr>

  def itemRow(name: String, percentage: Option[Int], href: String): Node =
    itemRow(Text(name), percentage, href)

  def itemRow(name: NodeSeq, percentage: Option[Int], href: String): Node =
    <tr>
      <td class="barContainerLeft"><a href={ href }>{ name }</a></td>
      <td class="barContainerRight">{ percentageBar(percentage) }</td>
    </tr>

  def percentageBar(percentage: Option[Int]) =
    <div class="percentages">
      <div class="bar">
        <div class="percentage">{ format(percentage) }</div>
        <div class="greenBar" style={ "width:%spx;".format(percentage.getOrElse(0)*2) }>&nbsp;</div>
      </div>
    </div>

  def format(percentage: Option[Int]) = percentage.getOrElse(0).toString + " %"

  def classItemRows(data: CoverageData) = {
    (for ((clazz, classData) <- data.forClasses) yield classItemRow(clazz, classData)).toList
  }
  def classItemRow(name: Name, classData: CoverageData) = {
    itemRow(classNameHeader(name), classData.percentage, classHref(name))
  }
  def classHref(name: Name) = sourceReportFileName(name.sourceFile) + "#" + toHtmlId(name)

  def classNameHeader(name: Name) = {
    val img = name.classType match {
      case ClassTypes.Object => "object.png"
      case ClassTypes.Trait => "trait.png"
      case ClassTypes.Class => "class.png"
      case ClassTypes.Root => "package.png"
      case ClassTypes.Package => "package.png"
    }
    <img src={img}/> ++ Text(name.className)
  }
  def toHtmlId(n: Name) =
    (n.classType.toString + "_" + n.packageName.replace(".", "_") + "_" + n.className.replace(".", "_")).replace("<", "_").replace(">","_")

}