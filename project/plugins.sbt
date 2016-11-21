logLevel := Level.Warn

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value