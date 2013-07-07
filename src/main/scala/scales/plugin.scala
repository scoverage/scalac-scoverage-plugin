package scales

import scala.tools.nsc.plugins.{PluginComponent, Plugin}
import scala.tools.nsc.Global
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL

/** @author Stephen Samuel */
class ScalesPlugin(val global: Global) extends Plugin {
    val name: String = "scales_coverage_plugin"
    val components: List[PluginComponent] = List(new ScalesComponent(global))
    val description: String = "code coverage compiler plugin"
}

class ScalesComponent(val global: Global) extends PluginComponent with TypingTransformers with Transform with TreeDSL {

    import global._

    override def newPhase(prev: scala.tools.nsc.Phase): Phase = new Phase(prev) {
        override def run: Unit = {
            // run after the transformer
            super.run
            println("Coverage completed")
            println(Instrumentation.instructions)
        }
    }
    val phaseName: String = "coverage inspector phase"
    val runsAfter: List[String] = List("typer")
    protected def newTransformer(unit: CompilationUnit): CoverageTransformer = new CoverageTransformer(unit)

    class CoverageTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {

        import global._

        def _safeStart(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.startOrPoint else -1
        def _safeLine(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.safeLine else -1
        def _safeSource(tree: Tree): Option[String] = if (tree.pos.isDefined) Some(tree.pos.source.path) else None

        override def transform(tree: Tree) = {
            // println(showRaw(tree))
            //tree
            process(tree)
        }

        // instrument the given case defintions not changing the patterns or guards
        def transformCases(cases: List[CaseDef]): List[CaseDef] = {
            cases.map(c => treeCopy.CaseDef(c, c.pat, c.guard, instrument(c.body)))
        }

        // wraps the given tree with an Instrumentation call
        def instrument(tree: Tree) = {
            val instruction = Instrumentation.add(_safeSource(tree), _safeStart(tree), _safeLine(tree))
            val apply = invokeCall(instruction.id)
            localTyper.typed(atPos(tree.pos)(apply))
        }

        def invokeCall(id: Int) =
            Apply(Select(Select(Ident("scales"), newTermName("Instrumentation")), newTermName("invoked")), List(Literal(Constant(id))))

        //        val instrumented = localTyper.typed(BLOCK(apply))
        //      treeCopy.DefDef(dd, dd.mods, dd.name, dd.tparams, dd.vparamss, dd.tpt, instrumented)

        def process(tree: Tree): Tree = {

            tree match {

                case _: PackageDef => super.transform(tree)
                case _: ClassDef => super.transform(tree)
                case _: Template => super.transform(tree)
                case _: TypeTree => super.transform(tree)
                case _: If => super.transform(tree)
                case _: Ident => super.transform(tree)
                case _: Block => super.transform(tree)
                case EmptyTree => super.transform(tree)

                case d: DefDef if tree.symbol.isConstructor && (tree.symbol.isTrait || tree.symbol.isModule) => tree
                case d: DefDef if tree.symbol.isConstructor => tree
                case d: DefDef if d.symbol.isCaseAccessor => tree
                case d: DefDef if tree.symbol.isDeferred => tree // abstract methods
                case d: DefDef if d.symbol.isSynthetic => tree // such as auto generated hash code methods in case classes
                case _: DefDef => super.transform(tree)

                case m: ModuleDef if m.symbol.isSynthetic => tree // a generated object, such as case class companion
                case _: ModuleDef => super.transform(tree)

                case v: ValDef if v.symbol.isParamAccessor => tree
                case v: ValDef if v.symbol.isCaseAccessor => tree
                case v: ValDef => treeCopy.ValDef(tree, v.mods, v.name, v.tpt, transform(v.rhs))

                case a: Assign =>
                    println("Assign: " + a)
                    a

                case Match(clause: Tree, cases: List[CaseDef]) => treeCopy.Match(tree, instrument(clause), transformCases(cases))
                case Try(t: Tree, cases: List[CaseDef], f: Tree) => treeCopy.Try(tree, instrument(t), transformCases(cases), instrument(f))

                case apply: Apply =>
                    println("Instrumenting apply " + apply)
                    instrument(apply)
                //                case literal: Literal => instrument(literal)
                //    case select: Select => instrument(select)

                case _ =>
                    println("Unrecognized construct sending to parent: " + tree.getClass + " " + tree.symbol)
                    super.transform(tree)
            }
        }
    }
}


