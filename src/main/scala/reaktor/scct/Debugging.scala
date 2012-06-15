package reaktor.scct

import java.net.URLClassLoader
import collection.JavaConversions

object Debugging {
  def props = {
    "System properties:"+JavaConversions.propertiesAsScalaMap(System.getProperties).map(e => e._1 + " = " + e._2).mkString("\n\t", "\n\t", "\n")
  }
  def classPath = {
    "Classpath:"+System.getProperty("java.class.path").split(":").mkString("\n\t", "\n\t", "\n")
  }

  def classLoaders = {
    classLoader("", getClass.getClassLoader, List())
  }

  def classLoader(tab: String, cl: ClassLoader, acc: List[String]): String = {
    if (cl == null) {
      acc.reverse.mkString("\n")
    } else {
      val curr = cl match {
        case urlCl: URLClassLoader => tab + cl.getClass.getName+":"+urlCl.getURLs.map(_.toString).mkString("\n\t"+tab, "\n\t"+tab, "\n")
        case _ => tab + cl.getClass.getName
      }
      classLoader(tab + "\t", cl.getParent, curr :: acc)
    }
  }
}