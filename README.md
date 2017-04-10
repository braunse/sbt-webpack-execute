# sbt-webpack-execute

This is a simple SBT plugin to execute webpack and add the
generated files to the build as resources.
This plugin does *not* integrate with
[sbt-web](https://github.com/sbt/sbt-web).
If you are looking for such an integration, look
[here](https://github.com/stejskal/sbt-webpack).

## Usage

In `project/webpack.sbt`:

    lazy val root = project.in(file("."))
      .dependsOn(url("git://github.com/braunse/sbt-webpack-execute.git"))

And in `build.sbt`:

    lazy val project = project.in(root_dir)
      .enablePlugins(WebpackExecutePlugin)

## Caveats

If you get file not found errors, make sure your webpack
configuration uses absolute file names for all input files.

Do not specify an output folder in `webpack.config.js`.
You can use the `Webpack.outputPath` setting to specify
which resource package the files should be output into.
