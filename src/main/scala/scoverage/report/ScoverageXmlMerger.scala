package scoverage.report

import scala.xml.Node

/** @author Stephen Samuel */
object ScoverageXmlMerger {

  /**
   * Merge the contents of the scoverage XML nodes into a single node.
   */
  def merge(nodes: Seq[Node]): Node = {
    def merge(node1: Node, node2: Node): Node = {
      val statementCount = (node1 \ "@statement-count").text.toInt + (node2 \ "@statement-count").text.toInt
      val statementsInvoked = (node1 \ "@statements-invoked").text.toInt + (node2 \ "@statements-invoked").text.toInt
      val statementRate = "%.2f".format(statementsInvoked.toDouble / statementCount.toDouble * 100.0d)
      val packages = (node1 \\ "packages") ++ (node2 \\ "packages")
      <scoverage statement-count={statementCount.toString}
                 statements-invoked={statementsInvoked.toString}
                 statement-rate={statementRate}
                 version="1.0"
                 timestamp={System.currentTimeMillis.toString}>
        {packages}
      </scoverage>
    }
    nodes.foldLeft(<scoverage statement-count="0" statements-invoked="0"/>: Node)((b, node) => merge(b, node))
  }
}
