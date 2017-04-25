lazy val root = project.in(file("."))
  .enablePlugins(WebpackExecutePlugin)
  .settings(
    version := "0.1",
    scalaVersion := "2.10.6",
    Webpack.outputPath := "my/pack",
    Webpack.configurationFile in Compile :=
      baseDirectory.value / "webpack-config-prod.js"
  )

