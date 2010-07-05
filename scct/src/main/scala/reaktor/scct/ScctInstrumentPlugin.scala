package reaktor.scct

import tools.nsc.plugins.{PluginComponent, Plugin}
import tools.nsc.{Phase, Global}
import java.io.File
import tools.nsc.transform.{Transform, TypingTransformers}
import tools.nsc.symtab.Flags

class ScctInstrumentPlugin(val global: Global) extends Plugin {
  val name = "scct"
  val description = "Scala code coverage instrumentation plugin."
  val runsAfter = List("refchecks")
  val components = List(new ScctTransformComponent(global))
}

class ScctTransformComponent(val global: Global) extends PluginComponent with TypingTransformers with Transform {
  import global._

  val phaseName = "ScctTransformComponent"
  val runsAfter = List("refchecks")
  lazy val coverageFile = new File(global.settings.outdir.value, "coverage.data")

  var saveData = true
  var counter = 0L
  var data: List[CoveredBlock] = Nil

  def newTransformer(unit: CompilationUnit) = new ScctTransformer(unit)

  override def newPhase(prev: scala.tools.nsc.Phase): StdPhase = new Phase(prev) {
    override def run {
      clearMetadata
      super.run
      saveMetadata
    }
    private def clearMetadata {
      if (coverageFile.exists) coverageFile.delete
    }
    private def saveMetadata {
      if (saveData) {
        println("scct: Saving coverage data.")
        MetadataPickler.toFile(data, coverageFile)
      }
    }
  }

  def newId = { counter = counter + 1; counter.toString }

  class ScctTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    override def transformUnit(unit: CompilationUnit) {
      //treeBrowser.browse(List(unit))
      super.transformUnit(unit)
      //treeBrowser.browse(List(unit))
      maybeDebug(unit)
    }

    private def maybeDebug(unit: CompilationUnit) {
      val debug = System.getProperty("coverage.debug")
      if (debug != null && (debug.equals("true") || unit.source.file.path.endsWith(debug))) {
        treeBrowser.browse(List(unit))
      }
    }

    case class TreeState(skip: Boolean, isLazyDef: Boolean) {
      def toSkip = TreeState(true, isLazyDef)
      def toLazyDef = TreeState(skip, true)
    }

    var state = TreeState(false, false)

    override def transform(t: Tree): Tree = {
      val prevState = state
      state = checkTreeState(t)
      val tree = super.transform(t)
      val result = if (state.skip) placeHolderize(tree) else instrument(tree)
      state = prevState
      result
    }

    private def placeHolderize(t: Tree) = t match {
      case classDef:ClassDef if !t.symbol.hasFlag(Flags.SYNTHETIC) => addPlaceHolder(classDef)
      case _ => t
    }

    private def checkTreeState(t: Tree): TreeState = {
      if (!t.hasSymbol) {
        state
      } else {
        val s = t.symbol
        def isGenerated = (s.hasFlag(Flags.STABLE) && !s.hasFlag(Flags.LAZY)) || (s.hasFlag(Flags.SYNTHETIC) && !s.isAnonymousFunction)
        def isGeneratedCaseClassMethod = s.isMethod && !s.isConstructor && t.pos.point == currentClass.pos.point
        def isObjectConstructor = s.isConstructor && currentClass.isModuleClass
        def isNonLazyAccessor = !s.hasFlag(Flags.LAZY) && s.hasFlag(Flags.ACCESSOR | Flags.PARAMACCESSOR)
        def hasSkipAnnotation = s.hasAnnotation(definitions.getClass("reaktor.scct.uncovered"))
        def isLazyDef = t match {
          case d:DefDef => s.isMethod && s.hasFlag(Flags.LAZY)
          case _ => false
        }

        if (isGenerated || hasSkipAnnotation || isNonLazyAccessor || isGeneratedCaseClassMethod || isObjectConstructor) {
          state.toSkip
        } else if (isLazyDef) {
          state.toLazyDef
        } else {
          state
        }
      }
    }

    private def instrument(tree: Tree) = tree match {
      // Don't place instrumentation before super call in constructors:
      // TODO: check if this is actually a constructor, currently it hits e.g. "try { super.hashCode } catch { ... }"
      case Block(stats @ List(Apply(Select(Super(_,_),_), args)), expr @ Literal(Constant(()))) =>
        treeCopy.Block(tree, stats ::: List(coverageCall(tree)), expr)

      // Skip generated lazy def accessor, since later phases assume { x = <rhs>; x }-tree-structure
      case Block(List(a @ Assign(lhs @ Select(_, _), rhs)), expr: Select) if state.isLazyDef => {
        val newAssign = fitIntoTree(a, Assign(lhs, instrumentBody(rhs)))
        treeCopy.Block(tree, List(newAssign), expr)
      }
      case Block(List(a @ Assign(lhs @ Ident(_), rhs)), expr: Ident) if state.isLazyDef => {
        val newAssign = fitIntoTree(a, Assign(lhs, instrumentBody(rhs)))
        treeCopy.Block(tree, List(newAssign), expr)
      }
      // Skip empty block in e.g. Some("foo").map(System.getProperty)
      case Block(Nil, Function(_,_)) =>
        tree
      case Block(stats, expr) => {
        val instrumentedBlock = instrumentBlockStats(stats) ::: instrumentBlockExpr(expr)
        treeCopy.Block(tree, instrumentedBlock, expr)
      }
      // Skip generated ifs in do {} while's
      case If(cond, thenp @ Apply(i @ Ident(_), Nil), elsep @ Literal(Constant(()))) if i.symbol.isLabel =>
        tree
      case If(cond, thenp, elsep) =>
        treeCopy.If(tree, cond, instrumentBody(thenp), instrumentBody(elsep))
      case Function(vparams, body) =>
        treeCopy.Function(tree, vparams, instrumentBody(body))
      case Try(block, catches, finalizer) =>
        treeCopy.Try(tree, instrumentBody(block), catches, instrumentBody(finalizer))
      case CaseDef(pat, guard, body) =>
        treeCopy.CaseDef(tree, pat, guard, instrumentBody(body))
      case ValDef(mods, name, tpt, rhs) =>
        treeCopy.ValDef(tree, mods, name, tpt, instrumentBody(rhs))
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        treeCopy.DefDef(tree, mods, name, tparams, vparamss, tpt, instrumentBody(rhs))
      case classDef: ClassDef =>
        addPlaceHolder(classDef)
      case template @ Template(parents, self, body) => {
        treeCopy.Template(tree, parents, self, body.map(instrumentBody))
      }
      case _ => tree
    }

    private def instrumentBlockExpr(expr: Tree) = expr match {
      case Literal(Constant(())) => Nil
      case _:LabelDef => Nil
      case _:Block => Nil
      // TODO: No good, but need to fix while's somehow: case Apply(Ident(_), Nil) => Nil
      case _ => List[Tree](coverageCall(expr))
    }

    private def instrumentBlockStats(stats: List[Tree]) = stats.foldLeft(List[Tree]()) { (list, stat) =>
      stat match {
        case _:ValDef => list ::: List(stat)
        case _:DefDef => list ::: List(stat)
        case _:LabelDef => list ::: List(stat)
        case _:Block => list ::: List(stat)
        case _ => list ::: List(coverageCall(stat), stat)
      }
    }

    private def instrumentBody(t: Tree) = t match {
      case Select(_,_) => instrumentedBlock(t)
      case Apply(_,_) => instrumentedBlock(t)
      case TypeApply(_,_) => instrumentedBlock(t)
      case Literal(Constant(())) => t
      case Literal(_) => instrumentedBlock(t)
      case Ident(_) => instrumentedBlock(t)
      case Throw(_) => instrumentedBlock(t)
      case Return(_) => instrumentedBlock(t)
      case _ => t
    }

    private def instrumentedBlock(t: Tree) = {
      treeCopy.Block(t, List(coverageCall(t)), t)
    }

    private def addPlaceHolder(classDef: ClassDef) = {
      if (!currentOwner.hasFlag(Flags.SYNTHETIC)) register(newId, classDef, true)
      classDef
    }

    private def coverageCall(tree: Tree) = {
      val id = newId
      register(id, tree, false)
      fitIntoTree(tree, rawCoverageCall(id))
    }

    private def fitIntoTree(orig: Tree, newTree: Tree) = {
      localTyper.typed(atPos(orig.pos)(newTree))
    }

    private def rawCoverageCall(id: String) = {
      val fun = Select(
        Select(
          Select(
            Ident("reaktor"),
            newTermName("scct")
          ),
          newTermName("Coverage")
        ),
        newTermName("invoked")
      )
      Apply(fun, List(Literal(id)))
    }

    private def register(id: String, tree: Tree, placeHolder: Boolean) {
      //if (!placeHolder) println("\n\nregistering "+tree.pos.point+" -> "+findMinOffset(tree)+":\n"+tree)
      val fileName = tree.pos.source.file.file.getAbsolutePath
      val name = Name(fileName, classType, packageName, className(currentClass))
      data = new CoveredBlock(id, name, findMinOffset(tree), placeHolder) :: data
    }

    private def findMinOffset(tree: Tree) = new MinimumOffsetFinder().offsetFor(tree)

    private def classType = {
      if (currentOwner.isRoot)
        ClassTypes.Root
      else if (currentClass.isModule || currentClass.isModuleClass)
        ClassTypes.Object
      else if (currentClass.isTrait)
        ClassTypes.Trait
      else ClassTypes.Class
    }

    private def packageName = {
      if (currentOwner.isRoot) "<root>" else currentPackage.nameString
    }

    private def className(curr: Symbol): String =
      if (curr.isRoot)
        "<root>"
      else if (curr.isAnonymousClass)
        className(curr.owner.enclClass)
      else if (curr.owner.isPackageClass)
        curr.simpleName.toString
      else className(curr.owner.enclClass) + "." + curr.simpleName.toString //owner.fullNameString
  }

  class MinimumOffsetFinder extends Traverser {
    var min = Integer.MAX_VALUE
    override def traverse(tree: Tree) {
      if (tree.pos.isDefined) {
        val curr = tree.pos.startOrPoint
        if (curr < min) min = curr
      }
      super.traverse(tree)
    }
     
    def offsetFor(tree: Tree): Int = {
      min = Integer.MAX_VALUE
      super.apply(tree)
      min
    }
  }

}