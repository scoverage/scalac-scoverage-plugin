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
            location,
            id,
            safeStart(tree),
            safeEnd(tree),
            safeLine(tree),
            tree.toString(),
            Option(tree.symbol).map(_.fullNameString).getOrElse("<nosymbol>"),
            tree.getClass.getSimpleName,
            branch
          )
          coverage.add(statement)

          val apply = invokeCall(id)
          val block = Block(apply, tree)
          localTyper.typed(atPos(tree.pos)(block))
      }
    }

    def className(s: Symbol): String = {
      if (s.enclClass.isAnonymousFunction || s.enclClass.isAnonymousFunction)
        className(s.owner)
      else
        s.enclClass.fullNameString
    }

    def updateLocation(s: Symbol) {
      val classType = {
        if (s.owner.enclClass.isTrait) ClassType.Trait
        else if (s.owner.enclClass.isModule) ClassType.Object
        else ClassType.Class
      }
      location = Location(
        s.owner.enclosingPackage.fullName,
        className(s),
        s.fullNameString,
        Option(s.owner).map(_.fullNameString).getOrElse("<noowner>"),
        s.toString(),
        s.flagString,
        Option(s.owner).map(_.toString()).getOrElse("<noowner>"),
        Option(s.owner).map(_.defString).getOrElse("<noowner>"),
        classType,
        Option(s.owner.enclMethod.nameString).getOrElse("<none>")
      )
    }

    def process(tree: Tree): Tree = {
      if (tree.hasSymbol) {
        //  if (tree.symbol.isSynthetic)
        //  println("isDerivedValueClass!!!!: " + tree.symbol + " " + tree)
        //if (tree.symbol.is)
        //println("PRIMARY CONSTRUCTOR: " + tree.symbol + " " + tree)
      }
      tree match {

        case EmptyTree => super.transform(tree)

        case a: ApplyDynamic =>
          println("ApplyDynamic: " + a.toString() + " " + a.symbol)
          tree

        /** This AST node corresponds to the following Scala code: fun(args)
          * With the guard, we are checking for case only applications
          * eg Currency.apply("USD")
          * todo decide if we should instrument the outer call, or just the param applys
          */
        case a: Apply if a.symbol.isCaseApplyOrUnapply =>
          treeCopy.Apply(a, a.fun, transformStatements(a.args))

        /** This AST node corresponds to the following Scala code: fun(args)
          * todo decide if we should instrument the outer call, or just the param applys
          */
        case a: Apply =>
          treeCopy.Apply(a, a.fun, transformStatements(a.args))

        /** pattern match with syntax `Block(stats, expr)`.
          * This AST node corresponds to the following Scala code:
          *
          * { stats; expr }
          *
          * If the block is empty, the `expr` is set to `Literal(Constant(()))`.
          */
        case b: Block => super.transform(tree)
        //treeCopy.Block(b, transformStatements(b.stats), transform(b.expr))

        case _: Import => super.transform(tree)
        case p: PackageDef => super.transform(tree)

        // special support to ignore partial functions
        // todo re-enable but fix to only instrument the case statements of applyOrElse
        case c: ClassDef if c.symbol.isAnonymousFunction &&
          c.symbol.enclClass.superClass.nameString.contains("AbstractPartialFunction") => tree

        // scalac generated classes, we just instrument the enclosed methods/statments
        // the location would stay as the source class
        case c: ClassDef if c.symbol.isAnonymousClass || c.symbol.isAnonymousFunction =>
          super.transform(tree)

        case c: ClassDef =>
          updateLocation(c.symbol)
          super.transform(tree)

        case d: DefDef if d.symbol.isVariable =>
          println("DEF VAR: " + d.toString() + " " + d.symbol)
          tree

        // todo do we really want to ignore?
        case d: DefDef if d.symbol.isPrimaryConstructor => tree
        // todo definitely want to instrument user level constructors
        case d: DefDef if tree.symbol.isConstructor => tree

        /**
         * Case class accessors for vals
         * EG for case class CreditReject(req: MarketOrderRequest, client: ActorRef)
         * <stable> <caseaccessor> <accessor> <paramaccessor> def req: com.sksamuel.scoverage.samples.MarketOrderRequest
         * <stable> <caseaccessor> <accessor> <paramaccessor> def client: akka.actor.ActorRef
         */
        case d: DefDef if d.symbol.isCaseAccessor => tree

        // Compiler generated case apply and unapply. Ignore these
        case d: DefDef if d.symbol.isCaseApplyOrUnapply => tree

        case d: DefDef if d.symbol.isCase =>
          println("DEF CASE: " + d.toString() + " " + d.symbol)
          super.transform(tree)

        /**
         * Stable getters are methods generated for access to a top level val.
         * Should be ignored as this is compiler generated code. The val definition will be instrumented.
         *
         * Eg
         * <stable> <accessor> def MaxCredit: scala.math.BigDecimal = CreditEngine.this.MaxCredit
         * <stable> <accessor> def alwaysTrue: String = InstrumentLoader.this.alwaysTrue
         * <stable> <accessor> lazy def instruments: Set[com.sksamuel.scoverage.samples.Instrument] = { ... }
         */
        case d: DefDef if d.symbol.isStable && d.symbol.isGetter => tree

        /** Getters are auto generated and should be ignored.
          *
          * Eg
          * <accessor> def cancellable: akka.actor.Cancellable
          * <accessor> private def _clientName: String =
          */
        case d: DefDef if d.symbol.isGetter => tree

        case d: DefDef if d.symbol.isStable =>
          println("STABLE DEF: " + d.toString() + " " + d.symbol)
          super.transform(tree)

        /** Accessors are auto generated setters and getters.
          * Eg
          * <accessor> def cancellable: akka.actor.Cancellable = PriceEngine.this.cancellable
          * <accessor> def cancellable_=(x$1: akka.actor.Cancellable): Unit = PriceEngine.this.cancellable = x$1
          */
        case d: DefDef if d.symbol.isAccessor => tree

        case d: DefDef if d.symbol.isParamAccessor && d.symbol.isAccessor =>
          println("PARAM ACCESSOR: " + d.toString() + " " + d.symbol)
          tree

        // generated setters  todo check the accessor flag is not set on user setters
        case d: DefDef if d.symbol.isAccessor && d.symbol.isSetter =>
          println("PARAM isAccessor isSetter: " + d.toString() + " " + d.symbol)
          tree

        // was `abstract' for members | trait is virtual
        case d: DefDef if tree.symbol.isDeferred => tree

        /** eg
          * override <synthetic> def hashCode(): Int
          * <synthetic> def copy$default$1: com.sksamuel.scoverage.samples.MarketOrderRequest
          */
        case d: DefDef if d.symbol.isSynthetic => tree

        /** Match all remaining def definitions
          *
          * If the return type is not specified explicitly (i.e. is meant to be inferred),
          * this is expressed by having `tpt` set to `TypeTree()` (but not to an `EmptyTree`!).
          */
        case d: DefDef =>
          updateLocation(d.symbol)
          super.transform(tree)

        // handle function bodies. This AST node corresponds to the following Scala code: vparams => body
        case f: Function =>
          println("FUNCTION: " + f.toString() + " " + f.symbol)
          treeCopy.Function(tree, f.vparams, process(f.body))

        case _: Ident => super.transform(tree)

        case i: If =>
          treeCopy.If(i, process(i.cond), transformIf(i.thenp), transformIf(i.elsep))

        // labeldefs are never written natively in scala
        case l: LabelDef =>
          treeCopy.LabelDef(tree, l.name, l.params, transform(l.rhs))

        // profile access to a literal for function args todo do we need to do this?
        case l: Literal => instrument(l)

        // pattern match clauses will be instrumented per case
        case Match(clause: Tree, cases: List[CaseDef]) =>
          treeCopy.Match(tree, instrument(clause), transformCases(cases))

        // a synthetic object is a generated object, such as case class companion
        case m: ModuleDef if m.symbol.isSynthetic => super.transform(tree)

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
        case s: Select => super.transform(tree)

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

        case t: Template =>
          treeCopy.Template(tree, t.parents, t.self, transformStatements(t.body))

        case _: TypeTree => super.transform(tree)

        /**
         * This AST node corresponds to any of the following Scala code:
         *
         * mods `val` name: tpt = rhs
         *
         * mods `var` name: tpt = rhs
         *
         * mods name: tpt = rhs        // in signatures of function and method definitions
         *
         * self: Bar =>                // self-types
         *
         * <synthetic> val default: A1 => B1 =
         * <synthetic> val x1: Any = _
         */
        case v: ValDef if v.symbol.isSynthetic => tree

        /**
         * Vals declared in case constructors
         */
        case v: ValDef if v.symbol.isParamAccessor && v.symbol.isCaseAccessor => tree

        // user defined value statements, we will instrument the RHS
        // This includes top level non-lazy vals. Lazy vals are generated as stable defs.
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


