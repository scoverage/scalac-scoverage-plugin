package reaktor.scct.report

import collection.immutable.{SortedMap, TreeMap}
import reaktor.scct.{CoveredBlock, Name}

class CoverageData(val blocks: List[CoveredBlock]) {

  private def forSourceFile(sourceFile: String) =
    new CoverageData(blocks.filter(_.name.sourceFile == sourceFile).sort(_.offset < _.offset))

  def forSourceFiles: Map[String, CoverageData] = {
    val names = blocks.map(_.name.sourceFile).removeDuplicates
    names.foldLeft(stringMap) { (map, n) => map + (n -> forSourceFile(n)) }
  }

  private def forClass(name: Name) =
    new CoverageData(blocks.filter(_.name == name))

  def forClasses: Map[Name, CoverageData] = {
    val names = blocks.map(_.name).removeDuplicates
    names.foldLeft(nameMap) { (map, n) => map + (n -> forClass(n)) }
  }

  private def forPackage(packageName: String) =
    new CoverageData(blocks.filter(_.name.packageName == packageName))

  def forPackages: Map[String, CoverageData] = {
    val names = blocks.map(_.name.packageName).removeDuplicates
    names.foldLeft(stringMap) { (map, n) => map + (n -> forPackage(n)) }
  }

  private def nameMap: SortedMap[Name, CoverageData] = new TreeMap[Name, CoverageData]()
  private def stringMap: SortedMap[String, CoverageData] = new TreeMap[String, CoverageData]()

  lazy val percentage: Option[Int] = {
    blocks.filter(!_.placeHolder) match {
      case List() => None
      case list => {
        val sum = list.foldLeft(0) { (sum, b) => if (b.count > 0) sum + 1 else sum }
        Some((sum * 100) / list.size)
      }
    }
  }
}