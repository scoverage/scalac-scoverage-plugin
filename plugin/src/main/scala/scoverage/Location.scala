package scoverage

import scala.tools.nsc.Global

import scoverage.reporter.ClassType

object Location {

  def fromGlobal(global: Global): global.Tree => Option[reporter.Location] = {
    tree =>
      def packageName(s: global.Symbol): String = {
        s.enclosingPackage.fullName
      }

      def className(s: global.Symbol): String = {
        // anon functions are enclosed in proper classes.
        if (s.enclClass.isAnonymousFunction || s.enclClass.isAnonymousClass)
          className(s.owner)
        else s.enclClass.nameString
      }

      def classType(s: global.Symbol): ClassType = {
        if (s.enclClass.isTrait) ClassType.Trait
        else if (s.enclClass.isModuleOrModuleClass) ClassType.Object
        else ClassType.Class
      }

      def fullClassName(s: global.Symbol): String = {
        // anon functions are enclosed in proper classes.
        if (s.enclClass.isAnonymousFunction || s.enclClass.isAnonymousClass)
          fullClassName(s.owner)
        else s.enclClass.fullNameString
      }

      def enclosingMethod(s: global.Symbol): String = {
        // check if we are in a proper method and return that, otherwise traverse up
        if (s.enclClass.isAnonymousFunction) enclosingMethod(s.owner)
        else if (s.enclMethod.isPrimaryConstructor) "<init>"
        else Option(s.enclMethod.nameString).getOrElse("<none>")
      }

      def sourcePath(symbol: global.Symbol): String = {
        Option(symbol.sourceFile).map(_.canonicalPath).getOrElse("<none>")
      }

      Option(tree.symbol) map { symbol =>
        reporter.Location(
          packageName(symbol),
          className(symbol),
          fullClassName(symbol),
          classType(symbol),
          enclosingMethod(symbol),
          sourcePath(symbol)
        )
      }
  }
}
