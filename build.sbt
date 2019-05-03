import de.heikoseeberger.sbtheader.License

lazy val headerSettings = headerLicense := Some(License.ALv2("2018-2019", "Faiaz Sanaulla"))

lazy val `chronicler-spark` = project
  .in(file("."))
  .settings(Settings.common: _*)
  .aggregate(
    sparkRdd,
    sparkDs,
    sparkStreaming,
    sparkStructuredStreaming
  )

lazy val sparkRdd = project
  .in(file("modules/rdd"))
  .settings(headerSettings)
  .settings(Settings.common: _*)
  .settings(Settings.publish: _*)
  .settings(
    name := "chronicler-spark-rdd",
    libraryDependencies ++= Seq(
      Library.urlMng % Test
    ) ++ Library.core
  )
  .dependsOn(tests % "test->test")
  .enablePlugins(AutomateHeaderPlugin)

lazy val sparkDs = project
  .in(file("modules/ds"))
  .settings(headerSettings)
  .settings(Settings.common: _*)
  .settings(Settings.publish: _*)
  .settings(
    name := "chronicler-spark-ds",
    libraryDependencies ++= Seq(
      Library.ds, 
      Library.urlMng % Test
    )
  )
  .dependsOn(sparkRdd)
  .dependsOn(tests % "test->test")
  .enablePlugins(AutomateHeaderPlugin)

lazy val sparkStreaming = project
  .in(file("modules/streaming"))
  .settings(headerSettings)
  .settings(Settings.common: _*)
  .settings(Settings.publish: _*)
  .settings(
    name := "chronicler-spark-streaming",
    libraryDependencies ++= Seq(
      Library.streaming,
      Library.urlMng     % Test
    ),
    parallelExecution in Test := false
  )
  .dependsOn(sparkRdd)
  .dependsOn(tests % "test->test")
  .enablePlugins(AutomateHeaderPlugin)

lazy val sparkStructuredStreaming = project
  .in(file("modules/structured-streaming"))
  .settings(headerSettings)
  .settings(Settings.common: _*)
  .settings(Settings.publish: _*)
  .settings(
    name := "chronicler-spark-structured-streaming",
    libraryDependencies ++= Seq(
      Library.ds,
      Library.urlMng % Test
    ) ++ Library.core 
  )
  .dependsOn(tests % "test->test")
  .enablePlugins(AutomateHeaderPlugin)

lazy val tests = project
  .in(file("modules/testing"))
  .settings(Settings.common: _*)
  .settings(
    name := "chronicler-spark-testing",
    libraryDependencies ++= Library.itTesting
  )