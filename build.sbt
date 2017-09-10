import ReleaseTransformations._

name := "sbt-webpack-execute"

organization := "de.sebbraun.sbt"

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
crossSbtVersions := Seq("0.13.6", "1.0.0")

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
  Seq("-Xmx1024m", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

releaseProcess := {
  val versions = crossSbtVersions.value
  // Run steps once for each SBT version.
  // Required since releaseStepCommandAndRemaining("^ scripted") discards errors
  // for some reason.
  def cross(ss: ReleaseStep*) =
    versions.flatMap { v =>
      (releaseStepCommand("^^ " + v): ReleaseStep) +: ss
    }

  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions
  ) ++ cross(
    releaseStepCommand("test"),
    releaseStepCommand("scripted")
  ) ++ Seq[ReleaseStep](
    setReleaseVersion,                      // : ReleaseStep
    commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
    tagRelease                              // : ReleaseStep
  ) ++ cross(
    releaseStepCommand("publishSigned")
  ) ++ Seq[ReleaseStep](
    setNextVersion,                         // : ReleaseStep
    commitNextVersion,                      // : ReleaseStep
    pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
  )
}
