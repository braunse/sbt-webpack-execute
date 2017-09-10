/*
 * Copyright (c) 2016 Sébastien Braun <sebastien@sebbraun.de>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package braunse.webpackexecute

import sbt._
import Keys._
import sys.{process => P}

object WebpackExecutePlugin extends AutoPlugin {
  object autoImport {
    object Webpack {
      val configurationFile = SettingKey[File]("webpack-configuration-file", "Path to the webpack configuration file")
      val webpackExecutable = SettingKey[File]("webpack-executable", "Path to the webpack executable")
      val npmExecutable = SettingKey[String]("webpack-npm-executable", "NPM executable to use")
      val environment = SettingKey[String]("webpack-env", "Environment to pass to webpack")
      val environmentVariable = SettingKey[String]("webpack-env-variable", "Environment variable to pass environment in")
      val npmPackageJSON = SettingKey[File]("webpack-package-json", "Path to package.json file")
      val resourceDir = SettingKey[File]("webpack-resource-dir", "Path to generated resource directory")
      val outputPath = SettingKey[String]("webpack-output-path", "Path to webpack output (within generated resource folder)")
      val inputDirs = SettingKey[Seq[File]]("webpack-input-dirs", "Path where webpack input files are searched")

      val ensureNPMInstalled = TaskKey[NPMUpToDate]("webpack-ensure-npm-uptodate", "Ensure that npm install has been called")
      val generate = TaskKey[Seq[File]]("webpack-generate", "Generate webpack output files")
    }

  }

  import autoImport._

  case class NPMUpToDate(skipped: Boolean)

  private[this] def defaultBuildWebpackSettings: Seq[Def.Setting[_]] = Seq(
    Webpack.npmExecutable := "npm"
  )

  private[this] val defaultProjectWebpackSettings: Seq[Def.Setting[_]] = Seq(
    Webpack.environment := "development",
    Webpack.environmentVariable := "WEBPACK_ENV",
    Webpack.configurationFile := baseDirectory.value / "webpack.config.js",
    Webpack.webpackExecutable := baseDirectory.value / "node_modules/webpack/bin/webpack.js",
    Webpack.npmPackageJSON := baseDirectory.value / "package.json",
    Webpack.resourceDir := resourceManaged.value / "webpack",
    Webpack.inputDirs := Nil,
    Webpack.inputDirs ++= Seq(
      baseDirectory.value / "src/main/frontend",
      baseDirectory.value / "node_modules",
      baseDirectory.value / "bower_components"
    ),

    Webpack.ensureNPMInstalled := { npmInstallTask(Webpack.ensureNPMInstalled).value },
    Webpack.generate := { resourceGeneratorTask(Webpack.generate).dependsOn(Webpack.ensureNPMInstalled).value },
    resourceGenerators += Webpack.generate.taskValue,

    managedResourceDirectories += Webpack.resourceDir.value
  )

  override def requires: Plugins = plugins.JvmPlugin

  override def buildSettings: Seq[_root_.sbt.Def.Setting[_]] = defaultBuildWebpackSettings

  override def projectSettings: Seq[_root_.sbt.Def.Setting[_]] =
    inConfig(Compile)(defaultProjectWebpackSettings)

  private[this] def npmInstallTask(key: TaskKey[NPMUpToDate]) = Def.task {
    val logs = (streams in key).value.log
    val packageJson = (Webpack.npmPackageJSON in key).value
    val npmExe = (Webpack.npmExecutable in key).value
    val baseDir = (baseDirectory in key).value
    if(packageJson.exists()) {
      val command = List(npmExe, "install")
      logs.info("npm install")
      val retval = mkProcess(command, Some(baseDir)).!
      if(retval != 0) {
        sys.error(s"npm install returned $retval")
      }
      NPMUpToDate(skipped = false)
    } else {
      logs.debug("Skipping npm install")
      NPMUpToDate(skipped = true)
    }
  }

  private[this] def resourceGeneratorTask(key: TaskKey[Seq[File]]) = Def.task {
    val packageJson = (Webpack.npmPackageJSON in key).value
    val webpackConfig = (Webpack.configurationFile in key).value
    val logs = (streams in key).value.log
    val potentialInputs = (Webpack.inputDirs in key).value.filter(_.isDirectory()).flatMap(dir => (dir ** ExistsFileFilter).get).toSet +
      (Webpack.configurationFile in key).value
    val outputDirectory = (Webpack.resourceDir in key).value
    val outputPath = (Webpack.outputPath in key).value
    val envVar = (Webpack.environmentVariable in key).value
    val envVal = (Webpack.environment in key).value
    val exe = (Webpack.webpackExecutable in key).value
    val cwd = (baseDirectory in key).value
    val webpackCacheDir = (streams in key).value.cacheDirectory / "webpack"

    if(!packageJson.exists() || !webpackConfig.exists()) {
      logs.info("Skipping webpack call")
      Seq()
    }
    else {
      val cachedCompilation = FileFunction.cached(webpackCacheDir, FilesInfo.lastModified, FilesInfo.exists) { (in: Set[File]) =>
        IO.delete(outputDirectory)
        outputDirectory.mkdirs()

        val command = List(
          "node", exe.absolutePath,
          "--output-path", (outputDirectory / outputPath).absolutePath,
          "--config", webpackConfig.absolutePath
        )

        logs.info("Executing webpack")
        logs.debug(s"Calling webpack: `${command.mkString("", " ", "")}'")

        val exitVal = mkProcess(command, Some(cwd), envVar -> envVal).!
        if (exitVal != 0) {
          sys.error(s"webpack returned exit code ${exitVal}")
        }

        (outputDirectory ** ExistsFileFilter).get.filter(_.isFile()).toSet
      }

      val generatedFiles = cachedCompilation(potentialInputs).toSeq
      logs.debug(s"Generated files: ${generatedFiles.mkString("\n * ", "\n * ", "")}")
      generatedFiles
    }
  }

  private def mkProcess(command: Seq[String], cwd: Option[File], extraEnv: (String, String)*): P.ProcessBuilder = {
    val isWindows = sys.props("os.name").contains("Windows")
    val wrappedCommand =
      if (isWindows) Seq("cmd", "/c") ++ command
      else command
    P.Process(wrappedCommand, cwd, extraEnv: _*)
  }
}
