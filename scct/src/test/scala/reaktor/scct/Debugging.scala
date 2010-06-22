package reaktor.scct

import java.net.URLClassLoader

object Debugging {
  def showClassPath {
    println("Classpath:\n"+System.getProperty("java.class.path").split(":").mkString("\n"))
  }

  def showClassLoaders {
    showClassLoader(getClass.getClassLoader)
  }

  def showClassLoader(cl: ClassLoader) {
    if (cl == null) {
      println("Null classloader.")
    } else {
      cl match {
        case urlCl: URLClassLoader => println("URLClassLoader:\n"+urlCl.getURLs.map(_.toString).mkString("\n"))
        case _ => println("Odd classloader: "+cl.getClass)
      }
      showClassLoader(cl.getParent)
    }
  }
}