lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"
def scalaXmlDep(scalaV: String): List[ModuleID] =
  CrossVersion.partialVersion(scalaV) match {
    case Some((2, minor)) if minor <= 10 =>
      Nil
    case Some((2, 11 | 12)) =>
      List("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
    case _ =>
      List("org.scala-lang.modules" %% "scala-xml" % "1.1.0")
  }

def relaxOldScala: Seq[Setting[_]] = Seq(
  scalacOptions := {
    val old = scalacOptions.value
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 12 =>
        old filterNot Set(
          "-Yno-adapted-args"
        )
      case _ =>
        old filterNot (Set(
          "-Xfatal-warnings",
          "-deprecation",
          "-Ywarn-unused",
          "-Ywarn-unused-import"
        ).apply _)
    }
  }
)

lazy val root = (project in file("."))
  .aggregate(core, treeExample)
  .settings(
    inThisBuild(
      Seq(
        organization := "org.scala-sbt",
        organizationHomepage := Some(url("http://scala-sbt.org/")),
        homepage := Some(url("https://github.com/sbt/sbinary")),
        version := "0.5.0-SNAPSHOT",
        scalaVersion := "2.12.4",
        crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.4", "2.13.0-M4"),
        bintrayPackage := "sbinary",
        developers := List(
          Developer(
            "drmaciver",
            "David R. MacIver",
            "@drmaciver",
            url("https://github.com/DRMacIver")
          ),
          Developer("harrah", "Mark Harrah", "@harrah", url("https://github.com/harrah")),
          Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
        ),
        description := "Library for describing binary formats for Scala types",
        licenses := Seq("MIT" -> new URL("https://github.com/sbt/sbinary/blob/master/LICENSE")),
        scmInfo := Some(
          ScmInfo(url("https://github.com/sbt/sbinary"), "git@github.com:sbt/sbinary.git")
        )
      )),
    name := "SBinary Parent",
    skip in publish := true
  )

lazy val core = (project in file("core")).settings(
  name := "SBinary",
  relaxOldScala,
  Fmpp.templateSettings,
  mimaPreviousArtifacts := {
    Set.empty
  },
  libraryDependencies += scalacheck % Test,
  libraryDependencies ++= scalaVersion(scalaXmlDep).value,
  unmanagedSourceDirectories in Compile += {
    val base = (scalaSource in Compile).value.getParentFile
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v >= 13 =>
        base / s"scala-2.13+"
      case _ =>
        base / s"scala-2.13-"
    }
  },
  unmanagedResources in Compile += (baseDirectory map { _ / "LICENSE" }).value
)

lazy val treeExample = (project in (file("examples") / "bt"))
  .dependsOn(core)
  .settings(
    name := "SBinary Tree Example",
    relaxOldScala,
    skip in publish := true
  )
