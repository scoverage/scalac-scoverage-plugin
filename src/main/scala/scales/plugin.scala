package scales

import scala.tools.nsc.plugins.{PluginComponent, Plugin}
import scala.tools.nsc.Global
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL
import scales.report.ScalesHtmlWriter
import scala.reflect.internal.util.SourceFile

/** @author Stephen Samuel */
class ScalesPlugin(val global: Global) extends Plugin {
    val name: String = "scales_coverage_plugin"
    val components: List[PluginComponent] = List(new ScalesComponent(global))
    val description: String = "code coverage compiler plugin"
}

class ScalesComponent(val global: Global) extends PluginComponent with TypingTransformers with Transform with TreeDSL {

    import global._

    override def newPhase(prev: scala.tools.nsc.Phase): Phase = new Phase(prev) {
        // run after the transformer to output the report
        override def run: Unit = {
            super.run
            println("** Coverage completed **")
            println("Statement coverage: " + Instrumentation.coverage.statementCoverage)
            val writer = ScalesHtmlWriter
            writer.write(Instrumentation.coverage)
        }
    }

    val phaseName: String = "coverage inspector phase"
    val runsAfter: List[String] = List("typer")
    protected def newTransformer(unit: CompilationUnit): CoverageTransformer = new CoverageTransformer(unit)

    class CoverageTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {

        import global._

        var _package: String = null
        var _class: String = null

        def _safeStart(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.startOrPoint else -1
        def _safeLine(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.safeLine else -1
        def _safeSource(tree: Tree): Option[SourceFile] = if (tree.pos.isDefined) Some(tree.pos.source) else None

        override def transform(tree: Tree) = process(tree)
        def transformStatements(trees: List[Tree]): List[Tree] = trees.map(tree => process(tree))

        // instrument the given case defintions not changing the patterns or guards
        def transformCases(cases: List[CaseDef]): List[CaseDef] = {
            cases.map(c => treeCopy.CaseDef(c, c.pat, c.guard, instrument(c.body)))
        }

        // wraps the given tree with an Instrumentation call
        def instrument(tree: Tree) = {
            _safeSource(tree) match {
                case None => tree
                case Some(source) =>
                    val instruction = Instrumentation.add(source, _package, _class, _safeStart(tree), _safeLine(tree))
                    val apply = invokeCall(instruction.id)
                    localTyper.typed(atPos(tree.pos)(apply))
            }
        }

        def invokeCall(id: Int) =
            Apply(Select(Select(Ident("scales"),
                newTermName("Instrumentation")),
                newTermName("invoked")), List(Literal(Constant(id))))

        def setPackageAndClass(s: Symbol) {
            _package = s.owner.enclosingPackage.nameString
            _class = s.owner.fullNameString
        }

        def process(tree: Tree): Tree = {

            tree match {

                case _: Import => tree
                case _: PackageDef => super.transform(tree)
                case c: ClassDef => super.transform(tree)
                case t: Template => treeCopy.Template(tree, t.parents, t.self, transformStatements(t.body))
                case _: TypeTree => super.transform(tree)
                case _: If => super.transform(tree)
                case _: Ident => super.transform(tree)
                case _: Block => super.transform(tree)
                case EmptyTree => super.transform(tree)

                case f: Function => treeCopy.Function(tree, f.vparams, transform(f.body))

                case l: LabelDef => treeCopy.LabelDef(tree, l.name, l.params, transform(l.rhs))

                case d: DefDef if tree.symbol.isConstructor && (tree.symbol.isTrait || tree.symbol.isModule) => tree
                case d: DefDef if tree.symbol.isConstructor => tree
                case d: DefDef if d.symbol.isCaseAccessor => tree
                case d: DefDef if d.symbol.isStable && d.symbol.isAccessor => tree // hidden accessor methods
                case d: DefDef if d.symbol.isParamAccessor && d.symbol.isAccessor => tree // hidden setter methods
                case d: DefDef if tree.symbol.isDeferred => tree // abstract methods
                case d: DefDef if d.symbol.isSynthetic => tree // such as auto generated hash code methods in case classes
                case d: DefDef =>
                    setPackageAndClass(d.symbol)
                    super.transform(tree)

                case m: ModuleDef if m.symbol.isSynthetic => tree // a generated object, such as case class companion
                case m: ModuleDef => super.transform(tree)

                case v: ValDef if v.symbol.isParamAccessor && v.symbol.isCaseAccessor => tree // case param accessores
                case v: ValDef =>
                    setPackageAndClass(v.symbol)
                    treeCopy.ValDef(tree, v.mods, v.name, v.tpt, transform(v.rhs))

                case apply: Apply => instrument(apply)
                case assign: Assign => instrument(assign)

                case Match(clause: Tree, cases: List[CaseDef]) => treeCopy.Match(tree, instrument(clause), transformCases(cases))
                case Try(t: Tree, cases: List[CaseDef], f: Tree) => treeCopy.Try(tree, instrument(t), transformCases(cases), instrument(f))

                //       println("Instrumenting apply " + apply)

                //                case literal: Literal => instrument(literal)
                //    case select: Select => instrument(select)

                case _ =>
                    println("Unexpected construct: " + tree.getClass + " " + tree.symbol)
                    super.transform(tree)
            }
        }
    }
}


