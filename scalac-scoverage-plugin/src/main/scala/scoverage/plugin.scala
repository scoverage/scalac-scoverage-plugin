package scoverage

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import scala.reflect.internal.ModifierFlags
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform
import scala.tools.nsc.transform.TypingTransformers

/** @author Stephen Samuel */
class ScoveragePlugin(val global: Global) extends Plugin {

  override val name: String = "scoverage"
  override val description: String = "scoverage code coverage compiler plugin"
  private val (extraAfterPhase, extraBeforePhase) = processPhaseOptions(
    pluginOptions
  )
  val instrumentationComponent = new ScoverageInstrumentationComponent(
    global,
    extraAfterPhase,
    extraBeforePhase
  )
  override val components: List[PluginComponent] = List(
    instrumentationComponent
  )

  private def parseExclusionEntry(
      entryName: String,
      inOption: String
  ): Seq[String] =
    inOption
      .substring(entryName.length)
      .split(";")
      .map(_.trim)
      .toIndexedSeq
      .filterNot(_.isEmpty)

  override def init(opts: List[String], error: String => Unit): Boolean = {
    val options = new ScoverageOptions

    for (opt <- opts) {
      if (opt.startsWith("excludedPackages:")) {
        options.excludedPackages = parseExclusionEntry("excludedPackages:", opt)
      } else if (opt.startsWith("excludedFiles:")) {
        options.excludedFiles = parseExclusionEntry("excludedFiles:", opt)
      } else if (opt.startsWith("excludedSymbols:")) {
        options.excludedSymbols = parseExclusionEntry("excludedSymbols:", opt)
      } else if (opt.startsWith("dataDir:")) {
        options.dataDir = opt.substring("dataDir:".length)
      } else if (
        opt
          .startsWith("extraAfterPhase:") || opt.startsWith("extraBeforePhase:")
      ) {
        // skip here, these flags are processed elsewhere
      } else if (opt == "reportTestName") {
        options.reportTestName = true
      } else {
        error("Unknown option: " + opt)
      }
    }
    if (!opts.exists(_.startsWith("dataDir:")))
      throw new RuntimeException(
        "Cannot invoke plugin without specifying <dataDir>"
      )
    instrumentationComponent.setOptions(options)
    true
  }

  override val optionsHelp: Option[String] = Some(
    Seq(
      "-P:scoverage:dataDir:<pathtodatadir>                  where the coverage files should be written\n",
      "-P:scoverage:excludedPackages:<regex>;<regex>         semicolon separated list of regexs for packages to exclude",
      "-P:scoverage:excludedFiles:<regex>;<regex>            semicolon separated list of regexs for paths to exclude",
      "-P:scoverage:excludedSymbols:<regex>;<regex>          semicolon separated list of regexs for symbols to exclude",
      "-P:scoverage:extraAfterPhase:<phaseName>              phase after which scoverage phase runs (must be after typer phase)",
      "-P:scoverage:extraBeforePhase:<phaseName>             phase before which scoverage phase runs (must be before patmat phase)",
      "                                                      Any classes whose fully qualified name matches the regex will",
      "                                                      be excluded from coverage."
    ).mkString("\n")
  )

  // copied from scala 2.11
  private def pluginOptions: List[String] = {
    // Process plugin options of form plugin:option
    def namec = name + ":"
    global.settings.pluginOptions.value filter (_ startsWith namec) map (_ stripPrefix namec)
  }

  private def processPhaseOptions(
      opts: List[String]
  ): (Option[String], Option[String]) = {
    var afterPhase: Option[String] = None
    var beforePhase: Option[String] = None
    for (opt <- opts) {
      if (opt.startsWith("extraAfterPhase:")) {
        afterPhase = Some(opt.substring("extraAfterPhase:".length))
      }
      if (opt.startsWith("extraBeforePhase:")) {
        beforePhase = Some(opt.substring("extraBeforePhase:".length))
      }
    }
    (afterPhase, beforePhase)
  }
}

class ScoverageOptions {
  var excludedPackages: Seq[String] = Nil
  var excludedFiles: Seq[String] = Nil
  var excludedSymbols: Seq[String] = Seq(
    "scala.reflect.api.Exprs.Expr",
    "scala.reflect.api.Trees.Tree",
    "scala.reflect.macros.Universe.Tree"
  )
  var dataDir: String = IOUtils.getTempPath
  var reportTestName: Boolean = false
}

class ScoverageInstrumentationComponent(
    val global: Global,
    extraAfterPhase: Option[String],
    extraBeforePhase: Option[String]
) extends PluginComponent
    with TypingTransformers
    with Transform {

  import global._

  val statementIds = new AtomicInteger(0)
  val coverage = new Coverage

  override val phaseName: String = "scoverage-instrumentation"
  override val runsAfter: List[String] =
    List("typer") ::: extraAfterPhase.toList
  override val runsBefore: List[String] =
    List("patmat") ::: extraBeforePhase.toList

  /** Our options are not provided at construction time, but shortly after,
    * so they start as None.
    * You must call "setOptions" before running any commands that rely on
    * the options.
    */
  private var options: ScoverageOptions = new ScoverageOptions()
  private var coverageFilter: CoverageFilter = AllCoverageFilter

  private val isScalaJsEnabled: Boolean = {
    try {
      rootMirror.getClassIfDefined("scala.scalajs.js.Any") != NoSymbol
    } catch {
      case _: Throwable => false
    }
  }

  def setOptions(options: ScoverageOptions): Unit = {
    this.options = options
    coverageFilter = new RegexCoverageFilter(
      options.excludedPackages,
      options.excludedFiles,
      options.excludedSymbols
    )
    new File(options.dataDir).mkdirs() // ensure data directory is created
  }

  override def newPhase(prev: scala.tools.nsc.Phase): Phase = new Phase(prev) {

    override def run(): Unit = {
      reporter.echo(s"Cleaning datadir [${options.dataDir}]")
      // we clean the data directory, because if the code has changed, then the number / order of
      // statements has changed by definition. So the old data would reference statements incorrectly
      // and thus skew the results.
      IOUtils.clean(options.dataDir)

      reporter.echo("Beginning coverage instrumentation")
      super.run()
      reporter.echo(
        s"Instrumentation completed [${coverage.statements.size} statements]"
      )

      Serializer.serialize(coverage, Serializer.coverageFile(options.dataDir))
      reporter.echo(
        s"Wrote instrumentation file [${Serializer.coverageFile(options.dataDir)}]"
      )
      reporter.echo(s"Will write measurement data to [${options.dataDir}]")
    }
  }

  protected def newTransformer(unit: CompilationUnit): Transformer =
    new Transformer(unit)

  class Transformer(unit: global.CompilationUnit)
      extends TypingTransformer(unit) {

    import global._

    // contains the location of the last node
    var location: Location = _

    /** The 'start' of the position, if it is available, else -1
      * We cannot use 'isDefined' to test whether pos.start will work, as some
      * classes (e.g. scala.reflect.internal.util.OffsetPosition have
      * isDefined true, but throw on `start`
      */
    def safeStart(tree: Tree): Int =
      scala.util.Try(tree.pos.start).getOrElse(-1)
    def safeEnd(tree: Tree): Int = scala.util.Try(tree.pos.end).getOrElse(-1)
    def safeLine(tree: Tree): Int =
      if (tree.pos.isDefined) tree.pos.line else -1
    def safeSource(tree: Tree): Option[SourceFile] =
      if (tree.pos.isDefined) Some(tree.pos.source) else None

    def invokeCall(id: Int): Tree = {
      Apply(
        Select(
          Select(
            Ident("scoverage"),
            newTermName("Invoker")
          ),
          newTermName("invoked")
        ),
        Literal(
          Constant(id)
        ) ::
          Literal(
            Constant(options.dataDir)
          ) ::
          (if (options.reportTestName)
             List(
               Literal(
                 Constant(true)
               )
             )
           else Nil)
      )
    }

    override def transform(tree: Tree) = process(tree)

    def transformStatements(trees: List[Tree]): List[Tree] = trees.map(process)

    def transformForCases(cases: List[CaseDef]): List[CaseDef] = {
      // we don't instrument the synthetic case _ => false clause
      cases
        .dropRight(1)
        .map(c => {
          treeCopy.CaseDef(
            // in a for-loop we don't care about instrumenting the guards, as they are synthetically generated
            c,
            c.pat,
            process(c.guard),
            process(c.body)
          )
        }) ++ cases.takeRight(1)
    }

    def transformCases(cases: List[CaseDef]): List[CaseDef] = {
      cases.map(c => {
        treeCopy.CaseDef(
          c,
          c.pat,
          process(c.guard),
          process(c.body)
        )
      })
    }

    def instrument(
        tree: Tree,
        original: Tree,
        branch: Boolean = false
    ): Tree = {
      safeSource(tree) match {
        case None =>
          reporter.echo(
            s"[warn] Could not instrument [${tree.getClass.getSimpleName}/${tree.symbol}]. No pos."
          )
          tree
        case Some(source) =>
          val id = statementIds.incrementAndGet
          val statement = Statement(
            location,
            id,
            safeStart(tree),
            safeEnd(tree),
            safeLine(tree),
            original.toString,
            Option(original.symbol).fold("<nosymbol>")(_.fullNameString),
            tree.getClass.getSimpleName,
            branch
          )
          if (tree.pos.isDefined && !isStatementIncluded(tree.pos)) {
            coverage.add(statement.copy(ignored = true))
            tree
          } else if (isUndefinedParameterInScalaJs(tree.symbol)) {
            coverage.add(statement.copy(ignored = true))
            statementIds.decrementAndGet()
            tree
          } else {
            coverage.add(statement)

            val apply = invokeCall(id)
            val block = Block(List(apply), tree)
            localTyper.typed(atPos(tree.pos)(block))
          }
      }
    }

    // Copied from
    // https://github.com/scala-js/scala-js/blob/4619d906baef7feb5d0b6d555d5b33044669434e/compiler/src/main/scala/org/scalajs/nscplugin/GenJSCode.scala#L2696-L2721
    private def isJSDefaultParam(sym: Symbol): Boolean = {
      if (isCtorDefaultParam(sym)) {
        isJSCtorDefaultParam(sym)
      } else {
        sym.hasFlag(reflect.internal.Flags.DEFAULTPARAM) &&
        isJSType(sym.owner) && {
          /* If this is a default parameter accessor on a
           * non-native JS class, we need to know if the method for which we
           * are the default parameter is exposed or not.
           * We do this by removing the $default suffix from the method name,
           * and looking up a member with that name in the owner.
           * Note that this does not work for local methods. But local methods
           * are never exposed.
           * Further note that overloads are easy, because either all or none
           * of them are exposed.
           */
          def isAttachedMethodExposed = {
            val methodName = nme.defaultGetterToMethod(sym.name)
            val ownerMethod = sym.owner.info.decl(methodName)
            ownerMethod.filter(isExposed).exists
          }

          !isNonNativeJSClass(sym.owner) || isAttachedMethodExposed
        }
      }
    }

    private lazy val JSTypeAnnot =
      rootMirror.getRequiredClass("scala.scalajs.js.annotation.internal.JSType")
    private lazy val ExposedJSMemberAnnot = rootMirror.getRequiredClass(
      "scala.scalajs.js.annotation.internal.ExposedJSMember"
    )
    private lazy val JSNativeAnnotation =
      rootMirror.getRequiredClass("scala.scalajs.js.native")

    private def isJSType(sym: Symbol): Boolean =
      sym.hasAnnotation(JSTypeAnnot)

    def isNonNativeJSClass(sym: Symbol): Boolean =
      !sym.isTrait && isJSType(sym) && !sym.hasAnnotation(JSNativeAnnotation)

    private def isExposed(sym: Symbol): Boolean = {
      !sym.isBridge && {
        if (sym.isLazy)
          sym.isAccessor && sym.accessed.hasAnnotation(ExposedJSMemberAnnot)
        else
          sym.hasAnnotation(ExposedJSMemberAnnot)
      }
    }

    private def isJSCtorDefaultParam(sym: Symbol) = {
      isCtorDefaultParam(sym) &&
      isJSType(patchedLinkedClassOfClass(sym.owner))
    }

    private def patchedLinkedClassOfClass(sym: Symbol): Symbol = {
      /* Work around a bug of scalac with linkedClassOfClass where package
       * objects are involved (the companion class would somehow exist twice
       * in the scope, making an assertion fail in Symbol.suchThat).
       * Basically this inlines linkedClassOfClass up to companionClass,
       * then replaces the `suchThat` by a `filter` and `head`.
       */
      val flatOwnerInfo = {
        // inline Symbol.flatOwnerInfo because it is protected
        if (sym.needsFlatClasses)
          sym.info
        sym.owner.rawInfo
      }
      val result = flatOwnerInfo.decl(sym.name).filter(_ isCoDefinedWith sym)
      if (!result.isOverloaded) result
      else result.alternatives.head
    }

    private def isCtorDefaultParam(sym: Symbol) = {
      sym.hasFlag(reflect.internal.Flags.DEFAULTPARAM) &&
      sym.owner.isModuleClass &&
      nme.defaultGetterToMethod(sym.name) == nme.CONSTRUCTOR
    }

    def isUndefinedParameterInScalaJs(sym: Symbol): Boolean = {
      isScalaJsEnabled && sym != null && isJSDefaultParam(sym)
    }
    def isClassIncluded(symbol: Symbol): Boolean =
      coverageFilter.isClassIncluded(symbol.fullNameString)
    def isFileIncluded(source: SourceFile): Boolean =
      coverageFilter.isFileIncluded(source)
    def isStatementIncluded(pos: Position): Boolean =
      coverageFilter.isLineIncluded(pos)
    def isSymbolIncluded(symbol: Symbol): Boolean =
      coverageFilter.isSymbolIncluded(symbol.fullNameString)

    def updateLocation(t: Tree): Unit = {
      Location(global)(t) match {
        case Some(loc) => this.location = loc
        case _ =>
          reporter.warning(t.pos, s"[warn] Cannot update location for $t")
      }
    }

    def transformPartial(c: ClassDef): ClassDef = {
      treeCopy.ClassDef(
        c,
        c.mods,
        c.name,
        c.tparams,
        treeCopy.Template(
          c.impl,
          c.impl.parents,
          c.impl.self,
          c.impl.body.map {
            case d: DefDef if d.name.toString == "applyOrElse" =>
              d.rhs match {
                case Match(selector, cases) =>
                  treeCopy.DefDef(
                    d,
                    d.mods,
                    d.name,
                    d.tparams,
                    d.vparamss,
                    d.tpt,
                    treeCopy.Match(
                      // note: do not transform last case as that is the default handling
                      d.rhs,
                      selector,
                      transformCases(cases.init) :+ cases.last
                    )
                  )
                case _ =>
                  reporter.error(
                    c.pos,
                    "Cannot instrument partial function apply. Please file bug report"
                  )
                  d
              }
            case other => other
          }
        )
      )
    }

    def debug(t: Tree): Unit = {
      import scala.reflect.runtime.{universe => u}
      reporter.echo(
        t.getClass.getSimpleName + ": LINE " + safeLine(t) + ": " + u.showRaw(t)
      )
    }

    def traverseApplication(t: Tree): Tree = {
      t match {
        case a: ApplyToImplicitArgs =>
          treeCopy.Apply(
            a,
            traverseApplication(a.fun),
            transformStatements(a.args)
          )
        case Apply(Select(_, name), List(fun @ Function(params, body)))
            if name.toString == "withFilter" && fun.symbol.isSynthetic && fun.toString
              .contains("check$ifrefutable$1") =>
          t
        case a: Apply =>
          treeCopy.Apply(
            a,
            traverseApplication(a.fun),
            transformStatements(a.args)
          )
        case a: TypeApply =>
          treeCopy.TypeApply(
            a,
            traverseApplication(a.fun),
            transformStatements(a.args)
          )
        case s: Select =>
          treeCopy.Select(s, traverseApplication(s.qualifier), s.name)
        case i: Ident => i
        case t: This  => t
        case other    => process(other)
      }
    }

    private def isSynthetic(t: Tree): Boolean =
      Option(t.symbol).fold(false)(_.isSynthetic)
    private def isNonSynthetic(t: Tree): Boolean = !isSynthetic(t)
    private def containsNonSynthetic(t: Tree): Boolean =
      isNonSynthetic(t) || t.children.exists(containsNonSynthetic)

    def allConstArgs(args: List[Tree]) =
      args.forall(arg => arg.isInstanceOf[Literal] || arg.isInstanceOf[Ident])

    def process(tree: Tree): Tree = {
      tree match {

        //        // non ranged inside ranged will break validation after typer, which only kicks in for yrangepos.
        //        case t if !t.pos.isRange => super.transform(t)

        // ignore macro expanded code, do not send to super as we don't want any children to be instrumented
        case t
            if t.attachments.all
              .toString()
              .contains("MacroExpansionAttachment") =>
          t

        // ignore "meta" macro generated code
        case t if t.pos.isDefined && t.pos.source.path == "<macro>" => t

        //        /**
        //         * Object creation from new.
        //         * Ignoring creation calls to anon functions
        //         */
        //        case a: GenericApply if a.symbol.isConstructor && a.symbol.enclClass.isAnonymousFunction => tree
        //        case a: GenericApply if a.symbol.isConstructor => instrument(a)

        /** When an apply has no parameters, or is an application of purely literals or idents
          * then we can simply instrument the outer call. Ie, we can treat it all as one single statement
          * for the purposes of code coverage.
          * This will include calls to case apply.
          */
        case a: GenericApply if allConstArgs(a.args) => instrument(a, a)

        /** Applications of methods with non trivial args means the args themselves
          * must also be instrumented
          */
        //todo remove once scala merges into Apply proper
        case a: ApplyToImplicitArgs =>
          instrument(
            treeCopy.Apply(
              a,
              traverseApplication(a.fun),
              transformStatements(a.args)
            ),
            a
          )

        // handle 'new' keywords, instrumenting parameter lists
        case a @ Apply(s @ Select(New(tpt), name), args) =>
          instrument(treeCopy.Apply(a, s, transformStatements(args)), a)
        case a: Apply =>
          instrument(
            treeCopy.Apply(
              a,
              traverseApplication(a.fun),
              transformStatements(a.args)
            ),
            a
          )
        case a: TypeApply =>
          instrument(
            treeCopy.TypeApply(
              a,
              traverseApplication(a.fun),
              transformStatements(a.args)
            ),
            a
          )

        /** pattern match with syntax `Assign(lhs, rhs)`.
          * This AST node corresponds to the following Scala code:
          * lhs = rhs
          */
        case assign: Assign =>
          treeCopy.Assign(assign, assign.lhs, process(assign.rhs))

        /** pattern match with syntax `Block(stats, expr)`.
          * This AST node corresponds to the following Scala code:
          * { stats; expr }
          * If the block is empty, the `expr` is set to `Literal(Constant(()))`.
          */
        case b: Block =>
          treeCopy.Block(b, transformStatements(b.stats), transform(b.expr))

        // special support to handle partial functions
        case c: ClassDef
            if c.symbol.isAnonymousFunction &&
              c.symbol.enclClass.superClass.nameString.contains(
                "AbstractPartialFunction"
              ) =>
          if (isClassIncluded(c.symbol)) {
            transformPartial(c)
          } else {
            c
          }

        // scalac generated classes, we just instrument the enclosed methods/statements
        // the location would stay as the source class
        case c: ClassDef
            if c.symbol.isAnonymousClass || c.symbol.isAnonymousFunction =>
          if (isFileIncluded(c.pos.source) && isClassIncluded(c.symbol))
            super.transform(tree)
          else {
            c
          }

        case c: ClassDef =>
          if (isFileIncluded(c.pos.source) && isClassIncluded(c.symbol)) {
            updateLocation(c)
            super.transform(tree)
          } else {
            c
          }

        // ignore macro definitions in 2.11
        case DefDef(mods, _, _, _, _, _) if mods.isMacro => tree

        // this will catch methods defined as macros, eg def test = macro testImpl
        // it will not catch macro implementations
        case d: DefDef
            if d.symbol != null
              && d.symbol.annotations.nonEmpty
              && d.symbol.annotations.toString() == "macroImpl" =>
          tree

        // will catch macro implementations, as they must end with Expr, however will catch
        // any method that ends in Expr. // todo add way of allowing methods that return Expr
        case d: DefDef if d.symbol != null && !isSymbolIncluded(d.tpt.symbol) =>
          tree

        // we can ignore primary constructors because they are just empty at this stage, the body is added later.
        case d: DefDef if d.symbol.isPrimaryConstructor => tree

        /** Case class accessors for vals
          * EG for case class CreditReject(req: MarketOrderRequest, client: ActorRef)
          * <stable> <caseaccessor> <accessor> <paramaccessor> def req: com.sksamuel.scoverage.samples.MarketOrderRequest
          * <stable> <caseaccessor> <accessor> <paramaccessor> def client: akka.actor.ActorRef
          */
        case d: DefDef if d.symbol.isCaseAccessor => tree

        // Compiler generated case apply and unapply. Ignore these
        case d: DefDef if d.symbol.isCaseApplyOrUnapply => tree

        /** Lazy stable DefDefs are generated as the impl for lazy vals.
          */
        case d: DefDef
            if d.symbol.isStable && d.symbol.isGetter && d.symbol.isLazy =>
          updateLocation(d)
          treeCopy.DefDef(
            d,
            d.mods,
            d.name,
            d.tparams,
            d.vparamss,
            d.tpt,
            process(d.rhs)
          )

        /** Stable getters are methods generated for access to a top level val.
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
          * <synthetic> def <init>$default$3: Option[org.joda.time.LocalDate] @scala.annotation.unchecked.uncheckedVariance = scala.None
          */
        case d: DefDef if d.symbol.isSynthetic => tree

        /** Match all remaining def definitions
          *
          * If the return type is not specified explicitly (i.e. is meant to be inferred),
          * this is expressed by having `tpt` set to `TypeTree()` (but not to an `EmptyTree`!).
          */
        case d: DefDef =>
          updateLocation(d)
          treeCopy.DefDef(
            d,
            d.mods,
            d.name,
            d.tparams,
            d.vparamss,
            d.tpt,
            process(d.rhs)
          )

        case EmptyTree => tree

        // handle function bodies. This AST node corresponds to the following Scala code: vparams => body
        case f: Function =>
          treeCopy.Function(tree, f.vparams, process(f.body))

        case _: Ident => tree

        // the If statement itself doesn't need to be instrumented, because instrumenting the condition is
        // enough to determine if the If statement was executed.
        // The two procedures (then and else) are instrumented separately to determine if we entered
        // both branches.
        case i: If =>
          treeCopy.If(
            i,
            process(i.cond),
            instrument(process(i.thenp), i.thenp, branch = true),
            instrument(process(i.elsep), i.elsep, branch = true)
          )

        case _: Import => tree

        // labeldefs are never written natively in scala
        case l: LabelDef =>
          treeCopy.LabelDef(tree, l.name, l.params, transform(l.rhs))

        // profile access to a literal for function args todo do we need to do this?
        case l: Literal => instrument(l, l)

        // pattern match clauses will be instrumented per case
        case m @ Match(selector: Tree, cases: List[CaseDef]) =>
          // we can be fairly sure this was generated as part of a for loop
          if (
            selector.toString.contains("check$")
            && selector.tpe.annotations.mkString == "unchecked"
            && m.cases.last.toString == "case _ => false"
          ) {
            treeCopy.Match(tree, process(selector), transformForCases(cases))
          } else {
            // if the selector was added by compiler, we don't want to instrument it....
            // that usually means some construct is being transformed into a match
            if (Option(selector.symbol).exists(_.isSynthetic))
              treeCopy.Match(tree, selector, transformCases(cases))
            else
              // .. but we will if it was a user match
              treeCopy.Match(tree, process(selector), transformCases(cases))
          }

        // a synthetic object is a generated object, such as case class companion
        case m: ModuleDef if m.symbol.isSynthetic =>
          updateLocation(m)
          super.transform(tree)

        // user defined objects
        case m: ModuleDef =>
          if (isFileIncluded(m.pos.source) && isClassIncluded(m.symbol)) {
            updateLocation(m)
            super.transform(tree)
          } else {
            m
          }

        /** match with syntax `New(tpt)`.
          * This AST node corresponds to the following Scala code:
          *
          * `new` T
          *
          * This node always occurs in the following context:
          *
          * (`new` tpt).<init>[targs](args)
          *
          * For example, an AST representation of:
          *
          * new Example[Int](2)(3)
          *
          * is the following code:
          *
          * Apply(
          * Apply(
          * TypeApply(
          * Select(New(TypeTree(typeOf[Example])), nme.CONSTRUCTOR)
          * TypeTree(typeOf[Int])),
          * List(Literal(Constant(2)))),
          * List(Literal(Constant(3))))
          */
        case n: New => n

        case s @ Select(n @ New(tpt), name) =>
          instrument(treeCopy.Select(s, n, name), s)

        case p: PackageDef =>
          if (isClassIncluded(p.symbol))
            treeCopy.PackageDef(p, p.pid, transformStatements(p.stats))
          else p

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
        case s: Select if location == null => tree

        /** I think lazy selects are the LHS of a lazy assign.
          * todo confirm we can ignore
          */
        case s: Select if s.symbol.isLazy => tree

        case s: Select =>
          instrument(
            treeCopy.Select(s, traverseApplication(s.qualifier), s.name),
            s
          )

        case s: Super => tree

        // This AST node corresponds to the following Scala code:    qual.this
        case t: This => super.transform(tree)

        // This AST node corresponds to the following Scala code:    `throw` expr
        case t: Throw => instrument(tree, tree)

        // This AST node corresponds to the following Scala code: expr: tpt
        case t: Typed => super.transform(tree)

        // instrument trys, catches and finally as separate blocks
        case Try(t: Tree, cases: List[CaseDef], f: Tree) =>
          treeCopy.Try(
            tree,
            instrument(process(t), t, branch = true),
            transformCases(cases),
            if (f.isEmpty) f else instrument(process(f), f, branch = true)
          )

        // type aliases, type parameters, abstract types
        case t: TypeDef => super.transform(tree)

        case t: Template =>
          updateLocation(t)
          treeCopy.Template(
            tree,
            t.parents,
            t.self,
            transformStatements(t.body)
          )

        case _: TypeTree => super.transform(tree)

        /** We can ignore lazy val defs as they are implemented by a generated defdef
          */
        case v: ValDef if v.symbol.isLazy => tree

        /** <synthetic> val default: A1 => B1 =
          * <synthetic> val x1: Any = _
          */
        case v: ValDef if v.symbol.isSynthetic => tree

        /** Vals declared in case constructors
          */
        case v: ValDef if v.symbol.isParamAccessor && v.symbol.isCaseAccessor =>
          tree

        // we need to remove the final mod so that we keep the code in order to check its invoked
        case v: ValDef if v.mods.isFinal =>
          updateLocation(v)
          treeCopy.ValDef(
            v,
            v.mods.&~(ModifierFlags.FINAL),
            v.name,
            v.tpt,
            process(v.rhs)
          )

        /** This AST node corresponds to any of the following Scala code:
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
          updateLocation(v)
          treeCopy.ValDef(tree, v.mods, v.name, v.tpt, process(v.rhs))

        case _ =>
          reporter.warning(
            tree.pos,
            "BUG: Unexpected construct: " + tree.getClass + " " + tree.symbol
          )
          super.transform(tree)
      }
    }
  }
}
