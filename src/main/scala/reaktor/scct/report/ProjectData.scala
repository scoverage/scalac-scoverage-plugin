package reaktor.scct.report

import java.io.File
import reaktor.scct.{Env, CoveredBlock}

case class ProjectData(projectId: String, baseDir: File, sourceDir: File, data:List[CoveredBlock]) {
  def this(env: Env, data:List[CoveredBlock]) = this(env.projectId, env.baseDir, env.sourceDir, data)
  @transient lazy val coverage = new CoverageData(data)
  @transient lazy val sourceLoader = new SourceLoader(baseDir)
}
