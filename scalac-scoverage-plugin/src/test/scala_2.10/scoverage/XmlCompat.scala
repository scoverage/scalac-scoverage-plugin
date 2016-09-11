package scoverage

object XmlCompat {
implicit class NodeSeqOps(nodeSeq: scala.xml.NodeSeq)  {
    def \@(attributeName: String): String =  (nodeSeq \ ("@" + attributeName)).text
  }
}
