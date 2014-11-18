
/** @author Stephen Samuel */
object EnvSupport {

  // code credit: http://stackoverflow.com/a/19040660/2048448
  def setEnv(k: String, v: String): Unit = {
    try {
      val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
      val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
      theEnvironmentField.setAccessible(true)

      val variableClass = Class.forName("java.lang.ProcessEnvironment$Variable")
      val convertToVariable = variableClass.getMethod("valueOf", classOf[java.lang.String])
      convertToVariable.setAccessible(true)

      val valueClass = Class.forName("java.lang.ProcessEnvironment$Value")
      val convertToValue = valueClass.getMethod("valueOf", classOf[java.lang.String])
      convertToValue.setAccessible(true)

      val sampleVariable = convertToVariable.invoke(null, "")
      val sampleValue = convertToValue.invoke(null, "")
      val env = theEnvironmentField.get(null).asInstanceOf[java.util.Map[sampleVariable.type, sampleValue.type]]

      val variable = convertToVariable.invoke(null, k).asInstanceOf[sampleVariable.type]
      val value = convertToValue.invoke(null, v).asInstanceOf[sampleValue.type]
      env.put(variable, value)

      val theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
      theCaseInsensitiveEnvironmentField.setAccessible(true)
      val cienv = theCaseInsensitiveEnvironmentField.get(null).asInstanceOf[java.util.Map[String, String]]
      cienv.put(k, v)

    }
    catch {
      case e: NoSuchFieldException =>
        val classes = classOf[java.util.Collections].getDeclaredClasses
        val env = System.getenv()
        classes foreach (cl => {
          if ("java.util.Collections$UnmodifiableMap" == cl.getName) {
            val field = cl.getDeclaredField("m")
            field.setAccessible(true)
            val map = field.get(env).asInstanceOf[java.util.Map[String, String]]
            map.put(k, v)
          }
        })
		case e : ClassNotFoundException =>
    }
  }
}
