import ReleaseTransformations._

name := "sbt-webpack-execute"

organization := "de.sebbraun.sbt"

scalaVersion := "2.10.6"

licenses := Seq(
  "MIT" -> url("http://opensource.org/licenses/MIT")
)

developers := List(
  Developer("braunse",
    "Sébastien Braun",
    "sebastien@sebbraun.de",
    url("https://github.com/braunse"))
)

scmInfo := Some(ScmInfo(url("https://github.com/braunse/sbt-webpack-execute/tree/master"),
  connection = "scm:git:https://github.com/braunse/sbt-webpack-execute",
  devConnection = Some("scm:git:ssh://github.com:braunse/sbt-webpack-execute.git")))

homepage := Some(url("https://github.com/braunse/sbt-webpack-execute"))

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

publishMavenStyle := true

sbtPlugin := true

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
  Seq("-Xmx1024m", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  releaseStepInputTask(scripted),         // run SBT plugin tests
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  publishArtifacts,                       // : ReleaseStep, checks whether `publishTo` is properly set up
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)
