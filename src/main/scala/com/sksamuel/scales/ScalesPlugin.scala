package com.sksamuel.scales

import scala.tools.nsc
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import scala.tools.nsc._
import tools.nsc.transform.{Transform, TypingTransformers}
import nsc.ast.TreeDSL

/** @author Stephen Samuel */

class ScalesPlugin(val global: Global) extends Plugin {

    val name = "something2"
    val description = "Scala code coverage"
    val components = List(new ScalesComponent(global))
}

class ScalesComponent(val global: Global) extends PluginComponent with TypingTransformers with Transform with TreeDSL {

    import global._

    def newTransformer(unit: CompilationUnit) = new ScalesTransformer(unit)

    val runsAfter = List[String]("typer")
    override val runsBefore = List[String]("patmat")
    val phaseName: String = "ScalesInstrumentPhase"

    override def newPhase(prev: scala.tools.nsc.Phase): StdPhase = new Phase(prev) {
        override def run: Unit = {
            // run after the transformer
            super.run
            println("Coverage completed")
        }
    }

    class ScalesTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {

        import CODE._

        def foo = {
            println("Called id: " + 45)
            45
        }

        override def transform(tree: Tree) = {
            println(s"Process ${tree.getClass} - symbol=${tree.symbol}")
            process(tree)
        }

        def process(tree: Tree): Tree = {

            tree match {

                case dd: DefDef if !dd.symbol.isConstructor => {

                    val apply = Apply(Select(Select(Ident("scala"), newTermName("Predef")), newTermName("println")),
                        List(Literal(Constant(2))))
                    //       val apply = Apply(Ident("instrument"), List(Literal(Constant(6))))

                    // String = Apply(Apply(Select(Apply(Select(Apply(Select(Select(This(newTypeName("scala")), newTermName("Predef")), newTermName("intWrapper")), List(Literal(Constant(1)))), newTermName("to")), List(Literal(Constant(3)))), newTermName("map")), List(Function(List(ValDef(Modifiers(<param> <synthetic>), newTermName("x$1"), TypeTree(), EmptyTree)), Apply(Select(Ident(newTermName("x$1")), newTermName("$plus")), List(Literal(Constant(1))))))), List(Select(Select(This(newTypeName("immutable")), newTermName("IndexedSeq")), newTermName("canBuildFrom"))))

                    val instrumented = localTyper.typed(BLOCK(apply))
                    treeCopy.DefDef(dd, dd.mods, dd.name, dd.tparams, dd.vparamss, dd.tpt, instrumented)
                }
                case _ => super.transform(tree)
            }
        }
    }
}


