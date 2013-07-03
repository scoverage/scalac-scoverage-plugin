package com.sksamuel.scales

/** @author Stephen Samuel */
case class Instruction(id: Int, name: String, offset: Int) {
    var count = 0
    def inc: Unit = count = count + 1
}

