package scoverage

import java.io.File

trait MacroSupport {

  val macroSupportDeps = Seq(testClasses)

  private def testClasses: File = new File(s"./scalac-scoverage-plugin/target/scala-${ScoverageCompiler.ShortScalaVersion}/test-classes")

}
