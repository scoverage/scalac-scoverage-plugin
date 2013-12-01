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

    override def transform(tree: Tree) = process(tree)

    def transformIf(tree: Tree) = {
      instrument(process(tree), true)
    }

    def transformStatements(trees: List[Tree]): List[Tree] = trees.map(tree => process(tree))

    def transformCases(cases: List[CaseDef]): List[CaseDef] = {
      cases.map(c => {
        treeCopy.CaseDef(
          c, c.pat, process(c.guard), process(c.body)
        )
      })
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
          val block = Block(List(apply), tree)
          localTyper.typed(atPos(tree.pos)(block))
      }
    }

    def className(s: Symbol): String = {
      if (s.enclClass.isAnonymousFunction || s.enclClass.isAnonymousFunction)
        className(s.owner)
      else
        s.enclClass.fullNameString
    }

    def enclosingMethod(s: Symbol): String = {
      if (s.enclClass.isAnonymousFunction || s.enclClass.isAnonymousFunction)
        enclosingMethod(s.owner)
      else
        Option(s.owner.enclMethod.nameString).getOrElse("<none>")
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
        classType,
        enclosingMethod(s)
      )
    }

    def transformPartial(c: ClassDef): ClassDef = {
      treeCopy.ClassDef(
        c, c.mods, c.name, c.tparams,
        treeCopy.Template(
          c.impl, c.impl.parents, c.impl.self, c.impl.body.map {
            case d: DefDef if d.name.toString == "applyOrElse" =>
              d.rhs match {
                case Match(selector, cases) =>
                  treeCopy.DefDef(
                    d, d.mods, d.name, d.tparams, d.vparamss, d.tpt,
                    treeCopy.Match(
                      d.rhs, selector, transformCases(cases)
                    )
                  )
                case _ =>
                  println("Cannot instrument partial apply. Please file bug report")
                  d
              }
            case other => other
          }
        )
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
          println("ApplyDynamic not yet implemented. " + a.toString() + " " + a.symbol)
          tree

        case a: ApplyToImplicitArgs =>
          println("ApplyToImplicitArgs not yet implemented. " + a.toString() + " " + a.symbol)
          treeCopy.Apply(a, a.fun, transformStatements(a.args))
          a

        //        /** This AST node corresponds to the following Scala code: fun(args)
        //          * With the guard, we are checking for case only applications
        //          * eg Currency.apply("USD")
        //          * todo decide if we should instrument the outer call, or just the param applys
        //          */
        //        case a: Apply if a.symbol.isCaseApplyOrUnapply =>
        //          treeCopy.Apply(a, a.fun, transformStatements(a.args))

        /**
         * Object creation from new.
         * Ignoring creation calls to anon functions
         */
        case a: Apply if a.symbol.isConstructor && a.symbol.enclClass.isAnonymousFunction => tree
        case a: Apply if a.symbol.isConstructor => instrument(a)

        /**
         * When an apply has no parameters, or is an application of purely literals or idents
         * then we can instrument the outer call.
         * This will include calls to case apply.
         */
        case a: Apply if a.args.isEmpty => instrument(a)
        case a: Apply if a.args.forall(arg => arg.isInstanceOf[Literal] || arg.isInstanceOf[Ident]) =>
          instrument(a)

        /**
         * Applications of methods with non trivial args means the args themselves
         * must also be instrumented
         */
        case a: Apply =>
          //      println("APPLY fun=" + a.fun)
          treeCopy.Apply(a, a.fun, transformStatements(a.args))
          a

        case a: TypeApply =>
          treeCopy.TypeApply(a, a.fun, transformStatements(a.args))
          a

        /** pattern match with syntax `Assign(lhs, rhs)`.
          * This AST node corresponds to the following Scala code:
          * lhs = rhs
          */
        case assign: Assign => assign

        /** pattern match with syntax `Block(stats, expr)`.
          * This AST node corresponds to the following Scala code:
          *
          * { stats; expr }
          *
          * If the block is empty, the `expr` is set to `Literal(Constant(()))`.
          */
        case b: Block =>
          treeCopy.Block(b, transformStatements(b.stats), transform(b.expr))

        case _: Import => super.transform(tree)

        // special support to ignore partial functions
        case c: ClassDef if c.symbol.isAnonymousFunction &&
          c.symbol.enclClass.superClass.nameString.contains("AbstractPartialFunction") => transformPartial(c)

        // scalac generated classes, we just instrument the enclosed methods/statments
        // the location would stay as the source class
        case c: ClassDef if c.symbol.isAnonymousClass || c.symbol.isAnonymousFunction =>
          super.transform(tree)

        case c: ClassDef =>
          updateLocation(c.symbol)
          super.transform(tree)

        //        case d: DefDef if d.symbol.name.toString == "randomInstrument" =>
        //          //import scala.reflect.runtime.{universe => u}
        //          //println("QQQQQ: " + d.toString() + " " + d.symbol + " " + u.showRaw(d) + " CHILDREN=" + d
        //          //  .rhs.children.map(_.shortClass))
        //          println("QQQQ" + d.rhs.shortClass)
        //          println("QQQQ" + d.rhs.asInstanceOf[Block].stats)
        //          println("QQQQ" + d.rhs.asInstanceOf[Block].expr)
        //          val rhs = treeCopy.Block(d.rhs, d.rhs.asInstanceOf[Block].stats, instrument(d.rhs.asInstanceOf[Block].expr))
        //          treeCopy.DefDef(d, d.mods, d.name, d.tparams, d.vparamss, d.tpt, rhs)

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

        case d: DefDef if d.symbol.isStable && d.symbol.isGetter => tree

        /**
         * Stable getters are methods generated for access to a top level val.
         * Should be ignored as this is compiler generated code.
         *
         * Eg
         * <stable> <accessor> def MaxCredit: scala.math.BigDecimal = CreditEngine.this.MaxCredit
         * <stable> <accessor> def alwaysTrue: String = InstrumentLoader.this.alwaysTrue
         */
        case d: DefDef if d.symbol.isStable && d.symbol.isGetter => tree

        /** Accessors are auto generated setters and getters.
          * Eg
          * <accessor> private def _clientName: String =
          * <accessor> def cancellable: akka.actor.Cancellable = PriceEngine.this.cancellable
          * <accessor> def cancellable_=(x$1: akka.actor.Cancellable): Unit = PriceEngine.this.cancellable = x$1
          */
        case d: DefDef if d.symbol.isAccessor => tree

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

        case _: Ident => tree

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

        case n: New =>
          println("NEW " + n)
          super.transform(n)

        case p: PackageDef => super.transform(p)

        // This AST node corresponds to the following Scala code:  `return` expr
        case r: Return =>
          treeCopy.Return(r, transform(r.expr))

        /** pattern match with syntax `Select(qual, name)`.
          * This AST node corresponds to the following Scala code:
          *
          * qualifier.selector
          *
          * Should only be used with `qualifier` nodes which are terms, i.e. which have `isTerm` returning `true`.
          * Otherwise `SelectFromTypeTree` should be used instead.
          *
          * foo.Bar // represented as Select(Ident(<foo>), <Bar>)
          * Foo#Bar // represented as SelectFromTypeTree(Ident(<Foo>), <Bar>)
          */
        case s: Select if location == null => super.transform(s)
        case s: Select if location.classType == ClassType.Class => instrument(s)
        case s: Select => super.transform(s)
        //          def nested(s: Tree): Tree = {
        //            s.children.head match {
        //              case _: Select => nested(s.children.head)
        //              case _ => process(s.children.head)
        //            }
        //          }

        case s: Super => tree

        // This AST node corresponds to the following Scala code:    qual.this
        case t: This => super.transform(tree)

        // This AST node corresponds to the following Scala code:    `throw` expr
        case t: Throw => instrument(tree)

        // This AST node corresponds to the following Scala code: expr: tpt
        case t: Typed => super.transform(tree)

        // instrument trys, catches and finally as seperate blocks
        case Try(t: Tree, cases: List[CaseDef], f: Tree) =>
          treeCopy.Try(tree, instrument(process(t), true), transformCases(cases), instrument(process(f), true))

        // type aliases, type parameters, abstract types
        case t: TypeDef => super.transform(tree)

        case t: Template =>
          treeCopy.Template(tree, t.parents, t.self, transformStatements(t.body))

        case _: TypeTree => super.transform(tree)

        /**
         * <synthetic> val default: A1 => B1 =
         * <synthetic> val x1: Any = _
         */
        case v: ValDef if v.symbol.isSynthetic => tree

        /**
         * Vals declared in case constructors
         */
        case v: ValDef if v.symbol.isParamAccessor && v.symbol.isCaseAccessor => tree

        /**
         * This AST node corresponds to any of the following Scala code:
         *
         * mods `val` name: tpt = rhs
         * mods `var` name: tpt = rhs
         * mods name: tpt = rhs        // in signatures of function and method definitions
         * self: Bar =>                // self-types
         *
         * For user defined value statements, we will instrument the RHS.
         *
         * This includes top level non-lazy vals. Lazy vals are generated as stable defs.
         */
        case v: ValDef =>
          updateLocation(v.symbol)
          treeCopy.ValDef(tree, v.mods, v.name, v.tpt, process(v.rhs))

        case _ =>
          println("Unexpected construct: " + tree.getClass + " " + tree.symbol)
          super.transform(tree)
      }
    }
  }
}


