
val rtJarOpt = taskKey[Option[String]]("Path to rt.jar if it exists")
val javalibEntry = taskKey[String]("Path to rt.jar or \"jrt:/\"")

inThisBuild(Def.settings(
  crossScalaVersions := Seq("3.1.0"),
  scalaVersion := crossScalaVersions.value.head,

  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-encoding",
    "utf-8",
  ),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalacenter/tasty-mima"),
      "scm:git@github.com:scalacenter/tasty-mima.git",
      Some("scm:git:git@github.com:scalacenter/tasty-mima.git")
    )
  ),
  organization := "ch.epfl.scala",
  homepage := Some(url(s"https://github.com/scalacenter/tasty-mima")),
  licenses += (("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))),
  developers := List(
    Developer("sjrd", "Sébastien Doeraene", "sjrdoeraene@gmail.com", url("https://github.com/sjrd/")),
    Developer("bishabosha", "Jamie Thompson", "bishbashboshjt@gmail.com", url("https://github.com/bishabosha")),
  ),

  versionPolicyIntention := Compatibility.BinaryCompatible,
  // Ignore dependencies to internal modules whose version is like `1.2.3+4...` (see https://github.com/scalacenter/sbt-version-policy#how-to-integrate-with-sbt-dynver)
  versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r),
))

val commonSettings = Seq(
  Test / parallelExecution := false,

  // Skip `versionCheck` for snapshot releases
  versionCheck / skip := isSnapshot.value,
)

val strictCompileSettings = Seq(
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Yexplicit-nulls",
    "-Ysafe-init",
    "-source:future",
  ),
)

lazy val root = project.in(file("."))
  .aggregate(tastyMiMaInterface, tastyMiMa).settings(
    publish / skip := true,
  )

lazy val tastyMiMaInterface =
  project.in(file("tasty-mima-interface"))
    .settings(commonSettings)
    .settings(
      name := "tasty-mima-interface",
      autoScalaLibrary := false,
      crossPaths := false,
    )

lazy val tastyMiMa =
  project.in(file("tasty-mima"))
    .dependsOn(tastyMiMaInterface)
    .settings(commonSettings)
    .settings(strictCompileSettings)
    .settings(name := "tasty-mima")
    .settings(
      libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
      testFrameworks += new TestFramework("munit.Framework")
    )
    .settings(
      libraryDependencies += "ch.epfl.scala" %% "tasty-query" % "0.5.6",

      Test / rtJarOpt := {
        for (bootClasspath <- Option(System.getProperty("sun.boot.class.path"))) yield {
          val rtJarOpt = bootClasspath.split(java.io.File.pathSeparatorChar).find { path =>
            new java.io.File(path).getName() == "rt.jar"
          }
          rtJarOpt match {
            case Some(rtJar) =>
              rtJar
            case None =>
              throw new AssertionError(s"cannot find rt.jar in $bootClasspath")
          }
        }
      },

      Test / envVars ++= {
        val javalib = (Test / javalibEntry).value

        def makeClasspathVar(fullCp: Classpath): String =
          (javalib +: Attributed.data(fullCp).map(_.getAbsolutePath())).mkString(";")

        val cpLibV1 = makeClasspathVar((testLibV1 / Compile / fullClasspath).value)
        val cpLibV2 = makeClasspathVar((testLibV2 / Compile / fullClasspath).value)

        val tastyMiMaCp = Attributed.data((Compile / fullClasspath).value).map(_.getAbsolutePath()).mkString(";")

        Map(
          "TASTYMIMA_TEST_LIBV1_CLASSPATH" -> cpLibV1,
          "TASTYMIMA_TEST_LIBV2_CLASSPATH" -> cpLibV2,
          "TASTYMIMA_CLASSPATH" -> tastyMiMaCp,
        )
      }
    )
    .settings(
      fork := true,
      Test / javalibEntry := (Test / rtJarOpt).value.getOrElse("jrt:/modules/java.base/"),
    )

lazy val testLibSettings: Seq[Setting[_]] = Def.settings(
  commonSettings,
  strictCompileSettings,
  publish / skip := true,
)

lazy val testLibV1 =
  project.in(file("test-lib-v1"))
    .settings(
      testLibSettings,
      name := "test-lib-v1",
    )

lazy val testLibV2 =
  project.in(file("test-lib-v2"))
    .settings(
      testLibSettings,
      name := "test-lib-v2",
    )
