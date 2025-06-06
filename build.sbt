ThisBuild / scalaVersion     := "3.3.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "BlockingQueueTokenManager",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.11",
      "dev.zio" %% "zio-test" % "2.1.11" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

Compile / compile := (Compile / compile).dependsOn(Compile / scalafmtCheck).value
Test / compile := (Test / compile).dependsOn(Test / scalafmtCheck).value