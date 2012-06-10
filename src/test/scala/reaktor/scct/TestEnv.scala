package reaktor.scct

object TestEnv {

  def isSbt = classPathHas("sbt-launch")
  def isIdea = classPathHas("idea_rt.jar")

  def classPathHas(s:String) = System.getProperty("java.class.path").contains(s)
}