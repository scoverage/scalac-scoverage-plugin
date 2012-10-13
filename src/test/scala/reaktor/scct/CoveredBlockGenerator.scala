package reaktor.scct

object CoveredBlockGenerator {

  def blocks(hits: Boolean*) = {
    1.to(hits.size).map { i =>
      val b = block(i)
      if (hits(i-1)) b.increment else b
    }.toList
  }
  def block(i: Int) = new CoveredBlock("c1", i, blockName(i.toString), i, false)
  def blockName(s: String) = Name(s, ClassTypes.Class, s, s, s)

}