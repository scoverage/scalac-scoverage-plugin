package scales

import scala.tools.nsc.plugins.{PluginComponent, Plugin}
import scala.tools.nsc.Global
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL
import scala.reflect.internal.util.SourceFile

/** @author Stephen Samuel */
class ScalesPlugin(val global: Global) extends Plugin {
  val name: String = "scales-plugin"
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

      IOUtils.serialize(InstrumentationRuntime.coverage, Env.coverageFile)
      println("scales: Written coverage file to " + Env.coverageFile.getAbsolutePath)
    }
  }

  protected def newTransformer(unit: CompilationUnit): CoverageTransformer = new CoverageTransformer(unit)

  class CoverageTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {
    //InstrumentationRuntime.coverage.sources.append(unit.source)

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

    // Creates a call to the invoker
    def invokeCall(id: Int): Apply = {
      Apply(
        Select(
          Select(
            Ident("scales"),
            newTermName("Invoker")
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
      val classType = {
        if (s.owner.enclClass.isTrait) ClassType.Trait
        else if (s.owner.enclClass.isModule) ClassType.Object
        else ClassType.Class
      }
      location = Location(
        s.owner.enclosingPackage.fullName,
        s.owner.enclClass.fullNameString,
        classType,
        Option(s.owner.enclMethod.fullNameString)
      )
    }

    //def registerPackage(p: PackageDef): Unit = InstrumentationRuntime.coverage.packageNames.add(p.name.toString)
    //def registerClass(p: ClassDef): Unit = InstrumentationRuntime.coverage.classNames.append(p.name.toString)

    def process(tree: Tree): Tree = {

      tree match {

        case EmptyTree => super.transform(tree)

        case _: Import => tree
        case p: PackageDef =>
          super.transform(tree)

        // scalac generated classes, we just instrument the enclosed methods/statments
        // the location would stay as the source class
        case c: ClassDef if c.symbol.isAnonymousClass || c.symbol.isAnonymousFunction =>
          super.transform(tree)

        case c: ClassDef =>
          updateLocation(c.symbol)
          //registerClass(c)
          super.transform(tree)

        case t: Template => treeCopy.Template(tree, t.parents, t.self, transformStatements(t.body))

        case _: TypeTree => super.transform(tree)
        case _: If => super.transform(tree)
        case _: Ident => super.transform(tree)
        case _: Block => super.transform(tree)

        //todo literals are wrapped to make sure we access them, but wouldn't we do that from a val?
        case l: Literal => instrument(l)

        // handle function bodies
        case f: Function =>
          treeCopy.Function(tree, f.vparams, transform(f.body))

        case l: LabelDef =>
          println("what is a label? " + l)
          treeCopy.LabelDef(tree, l.name, l.params, transform(l.rhs))

        // type aliases, type parameters, abstract types
        case t: TypeDef => tree

        case d: DefDef if tree.symbol.isConstructor && (tree.symbol.isTrait || tree.symbol.isModule) => tree

        // todo handle constructors, as a method?
        case d: DefDef if tree.symbol.isConstructor => tree

        // ignore case accessors as they are generated
        case d: DefDef if d.symbol.isCaseAccessor => tree

        // ignore accessors as they are generated
        case d: DefDef if d.symbol.isStable && d.symbol.isAccessor => tree // hidden accessor methods

        // ignore accessors as they are generated
        case d: DefDef if d.symbol.isParamAccessor && d.symbol.isAccessor => tree

        // abstract methods ??
        case d: DefDef if tree.symbol.isDeferred => tree

        // generated methods
        case d: DefDef if d.symbol.isSynthetic => tree

        // user defined methods
        case d: DefDef =>
          updateLocation(d.symbol)
          super.transform(tree)

        // should only occur inside something we are instrumenting.
        case s: Select =>
          super.transform(tree)

        // a synthetic object is a generated object, such as case class companion
        case m: ModuleDef if m.symbol.isSynthetic => tree

        // user defined objects
        case m: ModuleDef =>
          updateLocation(m.symbol)
          super.transform(tree)

        // This AST node corresponds to the following Scala code:    qual.this
        case t: This => tree

        // case param accessors are auto generated
        case v: ValDef if v.symbol.isCaseAccessor => tree

        // user defined value statements, we will instrument the RHS
        case v: ValDef =>
          updateLocation(v.symbol)
          treeCopy.ValDef(tree, v.mods, v.name, v.tpt, process(v.rhs))

        case apply: Apply => instrument(apply)
        case tapply: TypeApply => instrument(tapply)
        case assign: Assign => instrument(assign)
        //    case select: Select => instrument(select)

        // pattern match clauses will be instrumented per case
        case Match(clause: Tree, cases: List[CaseDef]) =>
          treeCopy.Match(tree, clause, transformCases(cases))

        // instrument trys, catches and finally as seperate blocks
        case Try(t: Tree, cases: List[CaseDef], f: Tree) =>
          treeCopy.Try(tree, instrument(t), transformCases(cases), instrument(f))

        case _ =>
          println("Unexpected construct: " + tree.getClass + " " + tree.symbol)
          super.transform(tree)
      }
    }
  }
}


