package scoverage

import scala.tools.nsc.Global

case class Location(packageName: String,
                    className: String,
                    topLevelClass: String,
                    classType: ClassType,
                    method: String,
                    sourcePath: String) extends java.io.Serializable {
  val fqn = (packageName + ".").replace("<empty>.", "") + className
}

object Location {

  def apply(global: Global): global.Tree => Option[Location] = { tree =>

    def packageName(s: global.Symbol): String = {
      s.enclosingPackage.fullName
    }

    def className(s: global.Symbol): String = {
      // anon functions are enclosed in proper classes.
      if (s.enclClass.isAnonymousFunction || s.enclClass.isAnonymousClass) className(s.owner)
      else s.enclClass.nameString
    }

    def classType(s: global.Symbol): ClassType = {
      if (s.enclClass.isTrait) ClassType.Trait
      else if (s.enclClass.isModuleOrModuleClass) ClassType.Object
      else ClassType.Class
    }

    def topLevelClass(s: global.Symbol): String = {
      s.enclosingTopLevelClass.nameString
    }

    def enclosingMethod(s: global.Symbol): String = {
      // check if we are in a proper method and return that, otherwise traverse up
      if (s.enclClass.isAnonymousFunction ) enclosingMethod(s.owner)
      else if (s.enclMethod.isPrimaryConstructor) "<init>"
      else Option(s.enclMethod.nameString).getOrElse("<none>")
    }

    def sourcePath(symbol: global.Symbol): String = {
      Option(symbol.sourceFile).map(_.canonicalPath).getOrElse("<none>")
    }

    Option(tree.symbol) map {
      symbol =>
        Location(
          packageName(symbol),
          className(symbol),
          topLevelClass(symbol),
          classType(symbol),
          enclosingMethod(symbol),
          sourcePath(symbol))
    }
  }
}