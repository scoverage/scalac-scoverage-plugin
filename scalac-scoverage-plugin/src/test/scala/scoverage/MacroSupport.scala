package scoverage

import java.io.File

trait MacroSupport {

  val macroContextPackageName: String =
    if (ScoverageCompiler.ShortScalaVersion == "2.10") {
      "scala.reflect.macros"
    } else {
      "scala.reflect.macros.blackbox"
    }

  val macroSupportDeps = Seq(testClasses)

  private def testClasses: File = new File(
    s"./scalac-scoverage-plugin/target/scala-${ScoverageCompiler.ScalaVersion}/test-classes"
  )

}
