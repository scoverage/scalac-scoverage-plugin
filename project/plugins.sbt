addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.11.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.7")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.2.0")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.33")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
