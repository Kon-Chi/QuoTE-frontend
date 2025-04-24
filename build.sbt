import org.scalajs.linker.interface.ESVersion
import org.scalajs.linker.interface.ModuleSplitStyle

lazy val quote = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin) // Enable the Scala.js plugin in this project
  .settings(
    scalaVersion := "3.3.5",

    // Tell Scala.js that this is an application with a main method
    scalaJSUseMainModuleInitializer := true,

    /* Configure Scala.js to emit modules in the optimal way to
     * connect to Vite's incremental reload.
     * - emit ECMAScript modules
     * - emit as many small modules as possible for classes in the "livechart" package
     * - emit as few (large) modules as possible for all other classes
     *   (in particular, for the standard library)
     */
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("livechart"))
        )
        .withSourceMap(true)
    },

    // Output to Vite's expected directory
    cleanFiles += baseDirectory.value / "vite" / "assets",
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory := baseDirectory.value / "vite" / "assets",
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory := baseDirectory.value / "vite" / "assets",

    scalaJSLinkerConfig ~= { _.withESFeatures(_.withESVersion(ESVersion.ES2015)) },

    /* Depend on the scalajs-dom library.
     * It provides static types for the browser DOM APIs.
     */
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "in.nvilla" %%% "monadic-html" % "0.5.0-RC1",
    )
  )
