package reaktor.scct.report

import java.io.File
import reaktor.scct.{Env, CoveredBlock}

@SerialVersionUID(1L) case class ProjectData(projectId: String, baseDir: File, sourceDir: File, data:Array[CoveredBlock]) {
  def this(env: Env, data:List[CoveredBlock]) = this(env.projectId, env.baseDir, env.sourceDir, data.toArray)
  @transient lazy val coverage = new CoverageData(data.toList)
  @transient lazy val sourceLoader = new SourceLoader(baseDir)
}
