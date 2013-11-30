package scoverage

import scala.tools.nsc.plugins.{PluginComponent, Plugin}
import scala.tools.nsc.Global
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.ast.TreeDSL
import scala.reflect.internal.util.SourceFile
import java.util.concurrent.atomic.AtomicInteger

/** @author Stephen Samuel */
class ScoveragePlugin(val global: Global) extends Plugin {
  val name: String = "scoverage-plugin"
  val components: List[PluginComponent] = List(new ScoverageComponent(global))
  val description: String = "scoverage code coverage compiler plugin"
}

class ScoverageComponent(val global: Global)
  extends PluginComponent with TypingTransformers with Transform with TreeDSL {

  import global._

  val statementIds = new AtomicInteger(0)
  val coverage = new Coverage
  val phaseName: String = "scoverage-phase"
  val runsAfter: List[String] = List("typer")
  override val runsBefore = List[String]("patmat")

  override def newPhase(prev: scala.tools.nsc.Phase): Phase = new Phase(prev) {

    override def run(): Unit = {
      println("scoverage: Begin profiling phase")
      super.run()
      println("scoverage: Profiling transformation completed")
      println("scoverage: " + coverage.statements.size + " statements profiled")

      IOUtils.serialize(coverage, Env.coverageFile)
      println("scoverage: Written coverage file to " + Env.coverageFile.getAbsolutePath)
    }
  }

  protected def newTransformer(unit: CompilationUnit): CoverageTransformer = new CoverageTransformer(unit)

  class CoverageTransformer(unit: global.CompilationUnit) extends TypingTransformer(unit) {

    import global._

    var location: Location = null

    def safeStart(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.startOrPoint else -1
    def safeEnd(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.endOrPoint else -1
    def safeLine(tree: Tree): Int = if (tree.pos.isDefined) tree.pos.safeLine else -1
    def safeSource(tree: Tree): Option[SourceFile] = if (tree.pos.isDefined) Some(tree.pos.source) else None

    override def transform(tree: Tree) = process(tree)
    def transformStatements(trees: List[Tree]): List[Tree] = trees.map(tree => process(tree))

    // instrument the given case defintions not changing the patterns or guards
    def transformCases(cases: List[CaseDef]): List[CaseDef] = {
      cases.map(c => treeCopy.CaseDef(c, c.pat, process(c.guard), instrument(process(c.body), true)))
    }

    def transformIf(tree: Tree) = {
      instrument(process(tree), true)
    }

    def invokeCall(id: Int): Apply = {
      Apply(
        Select(
          Select(
            Ident("scoverage"),
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

    def instrument(tree: Tree, branch: Boolean = false) = {
      safeSource(tree) match {
        case None => tree
        case Some(source) =>

          val id = statementIds.incrementAndGet
          val statement = MeasuredStatement(
            source.path,
            location, id,
            safeStart(tree),
            safeEnd(tree),
            safeLine(tree),
            tree.toString(),
            branch
          )
          coverage.add(statement)

          val apply = invokeCall(id)
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
        Option(s.owner.enclMethod.nameString).getOrElse("<none>")
      )
    }

    def process(tree: Tree): Tree = {
      tree match {

        case EmptyTree => super.transform(tree)

        // This AST node corresponds to the following Scala code: fun(args)
        case apply: Apply =>
          instrument(treeCopy.Apply(apply, apply.fun, transformStatements(apply.args)))

        // just carry on as normal with a block, we'll process the children
        case b: Block =>
          treeCopy.Block(b, transformStatements(b.stats), transform(b.expr))

        case _: Import => super.transform(tree)
        case p: PackageDef => super.transform(tree)

        // scalac generated classes, we just instrument the enclosed methods/statments
        // the location would stay as the source class
        case c: ClassDef if c.symbol.isAnonymousClass || c.symbol.isAnonymousFunction =>
          super.transform(tree)

        case c: ClassDef =>
          updateLocation(c.symbol)
          super.transform(tree)

        case t: Template =>
          treeCopy.Template(tree, t.parents, t.self, transformStatements(t.body))

        case _: TypeTree => super.transform(tree)

        case d: DefDef if tree.symbol.isConstructor && (tree.symbol.isTrait || tree.symbol.isModule) => tree

        // todo handle constructors, as a method?
        case d: DefDef if tree.symbol.isConstructor => tree

        // ignore case param/accessors
        case d: DefDef if d.symbol.isCaseAccessor => tree

        // ignore accessors as they are generated
        case d: DefDef if d.symbol.isStable && d.symbol.isAccessor => tree // hidden accessor methods

        // for field definitions generated for primary constructor
        case d: DefDef if d.symbol.isParamAccessor && d.symbol.isAccessor => tree

        // generated setters // todo check the accessor flag is not set on user setters
        case d: DefDef if d.symbol.isAccessor && d.symbol.isSetter => tree

        // abstract methods ??
        case d: DefDef if tree.symbol.isDeferred => tree

        // generated methods
        case d: DefDef if d.symbol.isSynthetic =>
          super.transform(tree)

        case d: DefDef if d.symbol.isCaseApplyOrUnapply =>
          println("Case apply/unapply " + d)
          updateLocation(d.symbol)
          super.transform(tree)

        // user defined methods
        case d: DefDef =>
          updateLocation(d.symbol)
          super.transform(tree)

        case _: Ident =>
          super.transform(tree)

        case i: If =>
          treeCopy.If(i, process(i.cond), transformIf(i.thenp), transformIf(i.elsep))

        // handle function bodies. This AST node corresponds to the following Scala code: vparams => body
        case f: Function =>
          treeCopy.Function(tree, f.vparams, process(f.body))

        // labeldefs are never written natively in scala
        case l: LabelDef =>
          treeCopy.LabelDef(tree, l.name, l.params, transform(l.rhs))

        // profile access to a literal for function args todo do we need to do this?
        case l: Literal => instrument(l)

        // pattern match clauses will be instrumented per case
        case Match(clause: Tree, cases: List[CaseDef]) =>
          treeCopy.Match(tree, instrument(clause), transformCases(cases))

        // a synthetic object is a generated object, such as case class companion
        case m: ModuleDef if m.symbol.isSynthetic => tree

        // user defined objects
        case m: ModuleDef =>
          updateLocation(m.symbol)
          super.transform(tree)

        // This AST node corresponds to the following Scala code:  `return` expr
        case r: Return =>
          treeCopy.Return(r, transform(r.expr))

        // This AST node corresponds to the following Scala code: expr: tpt
        case t: Typed => super.transform(tree)

        // should only occur inside something we are instrumenting.
        case s: Select =>
          super.transform(tree)

        //case s: Super =>
        // treeCopy.Super(s, s.qual, s.mix)

        // This AST node corresponds to the following Scala code:    qual.this
        case t: This => super.transform(tree)

        // This AST node corresponds to the following Scala code:    `throw` expr
        case t: Throw => instrument(tree)

        // instrument trys, catches and finally as seperate blocks
        case Try(t: Tree, cases: List[CaseDef], f: Tree) =>
          treeCopy.Try(tree, instrument(process(t), true), transformCases(cases), instrument(process(f), true))

        // type aliases, type parameters, abstract types
        case t: TypeDef => super.transform(tree)

        // case param accessors are auto generated
        case v: ValDef if v.symbol.isCaseAccessor => super.transform(tree)

        // user defined value statements, we will instrument the RHS
        case v: ValDef =>
          updateLocation(v.symbol)
          treeCopy.ValDef(tree, v.mods, v.name, v.tpt, process(v.rhs))

        case tapply: TypeApply => instrument(tapply)
        case assign: Assign => instrument(assign)

        case _ =>
          println("Unexpected construct: " + tree.getClass + " " + tree.symbol)
          super.transform(tree)
      }
    }
  }
}


