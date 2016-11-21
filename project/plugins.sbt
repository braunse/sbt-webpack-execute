logLevel := Level.Warn

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value