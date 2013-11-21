package scales

import scala.tools.nsc.plugins.{PluginComponent, Plugin}
import scala.tools.nsc.Global
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL
import scala.reflect.internal.util.SourceFile

/** @author Stephen Samuel */
class ScalesPlugin(val global: Global) extends Plugin {
  val name: String = "scales_coverage_plugin"
  val components: List[PluginComponent] = List(new ScalesComponent(global))
  val description: String = "scales code coverage compiler plugin"
}

class ScalesComponent(val global: Global)
  extends PluginComponent with TypingTransformers with Transform with TreeDSL {

  import global._

  val phaseName: String = "scales-phase"
  val runsAfter: List[String] = List("typer")
  override val runsBefore = List[String]("patmat")

  override def newPhase(prev: scala.tools.nsc.Phase): Phase = new Phase(prev) {

    override def run(): Unit = {
      println("scales: Begin profiling phase")
      super.run()
      println("scales: Profiling transformation completed")
      println("scales: " + InstrumentationRuntime.coverage.statements.size + " statements profiled")
      println(InstrumentationRuntime.coverage.statements)
    }
  }

  protected def newTransformer(unit: CompilationUnit): CoverageTransformer = new CoverageTransformer(unit)

  class CoverageTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {
    InstrumentationRuntime.coverage.sources.append(unit.source)

    import global._

    var location: Location = null

    def safeStart(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.startOrPoint else -1
    def safeLine(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.safeLine else -1

    override def transform(tree: Tree) = process(tree)
    def transformStatements(trees: List[Tree]): List[Tree] = trees.map(tree => process(tree))

    // instrument the given case defintions not changing the patterns or guards
    def transformCases(cases: List[CaseDef]): List[CaseDef] = {
      cases.map(c => treeCopy.CaseDef(c, c.pat, c.guard, instrument(c.body)))
    }

    /**
     * Creates a call to invoked(id) which in turn sets the statement
     * with the given id to invoked.
     */
    def invokeCall(id: Int): Apply = {
      Apply(
        Select(
          Select(
            Ident("scales"),
            newTermName("InstrumentationRuntime")
          ),
          newTermName("invoked")
        ),
        List(
          Literal(
            Constant(id)
          )
        )
      )
    }

    def safeSource(tree: Tree): Option[SourceFile] = if (tree.pos.isDefined) Some(tree.pos.source) else None

    // wraps the given tree with an Instrumentation call
    def instrument(tree: Tree) = {
      safeSource(tree) match {
        case None => tree
        case Some(source) =>
          val instruction =
            InstrumentationRuntime.add(source, location, safeStart(tree), safeLine(tree), tree.toString())
          val apply = invokeCall(instruction.id)
          val block = Block(apply, tree)
          localTyper.typed(atPos(tree.pos)(block))
      }
    }

    def updateLocation(s: Symbol) {
      location = Location(s.owner.enclosingPackage.nameString,
        s.owner.enclClass.fullNameString,
        Option(s.owner.enclMethod.fullNameString))
    }

    //def registerPackage(p: PackageDef): Unit = InstrumentationRuntime.coverage.packageNames.add(p.name.toString)
    //def registerClass(p: ClassDef): Unit = InstrumentationRuntime.coverage.classNames.append(p.name.toString)

    def process(tree: Tree): Tree = {

      tree match {

        case EmptyTree => super.transform(tree)

        case _: Import => tree
        case p: PackageDef =>
          //registerPackage(p)
          super.transform(tree)

        case c: ClassDef if c.symbol.isAnonymousClass || c.symbol.isAnonymousFunction => super.transform(tree)
        case c: ClassDef =>
          updateLocation(c.symbol)
          //registerClass(c)
          super.transform(tree)

        case t: Template => treeCopy.Template(tree, t.parents, t.self, transformStatements(t.body))
        case _: TypeTree => super.transform(tree)
        case _: If => super.transform(tree)
        case _: Ident => super.transform(tree)
        case _: Block => super.transform(tree)

        case l: Literal => instrument(l)

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
          updateLocation(d.symbol)
          InstrumentationRuntime.coverage.methodNames.append(d.name.toString)
          super.transform(tree)

        case s: Select => super.transform(tree) // should only occur inside something we are instrumenting.

        case m: ModuleDef if m.symbol.isSynthetic => tree // a generated object, such as case class companion
        case m: ModuleDef => super.transform(tree)

        case v: ValDef if v.symbol.isParamAccessor && v.symbol.isCaseAccessor => tree // case param accessors
        case v: ValDef =>
          updateLocation(v.symbol)
          treeCopy.ValDef(tree, v.mods, v.name, v.tpt, transform(v.rhs))

        case apply: Apply => instrument(apply)
        case tapply: TypeApply => instrument(tapply)
        case assign: Assign => instrument(assign)

        case Match(clause: Tree, cases: List[CaseDef]) => treeCopy.Match(tree, clause, transformCases(cases))
        case Try(t: Tree, cases: List[CaseDef], f: Tree) =>
          treeCopy.Try(tree, instrument(t), transformCases(cases), instrument(f))

        //       println("Instrumenting apply " + apply)
        //    case select: Select => instrument(select)

        case _ =>
          println("Unexpected construct: " + tree.getClass + " " + tree.symbol)
          super.transform(tree)
      }
    }
  }
}


