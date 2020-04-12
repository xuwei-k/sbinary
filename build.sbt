import Dependencies._

ThisBuild / organization := "org.scala-sbt"
ThisBuild / organizationHomepage := Some(url("http://scala-sbt.org/"))
ThisBuild / homepage := Some(url("https://github.com/sbt/sbinary"))
ThisBuild / version := "0.5.1-SNAPSHOT"
ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala210, scala211, scala212, scala213)
ThisBuild / bintrayPackage := "sbinary"
ThisBuild / developers := List(
  Developer(
    "drmaciver",
    "David R. MacIver",
    "@drmaciver",
    url("https://github.com/DRMacIver")
  ),
  Developer("harrah", "Mark Harrah", "@harrah", url("https://github.com/harrah")),
  Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
)
ThisBuild / description := "Library for describing binary formats for Scala types"
ThisBuild / licenses := Seq("MIT" -> new URL("https://github.com/sbt/sbinary/blob/master/LICENSE"))
ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/sbt/sbinary"), "git@github.com:sbt/sbinary.git")
)

lazy val root = (project in file("."))
  .aggregate(core, treeExample)
  .settings(nocomma {
    name := "SBinary Root"
    publish / skip := true
    mimaPreviousArtifacts := Set.empty
  })

lazy val core = (project in file("core"))
  .settings(nocomma {
    name := "SBinary"

    mimaPreviousArtifacts := {
      Set.empty
    }
    libraryDependencies += scalacheck % Test
    libraryDependencies ++= scalaVersion(scalaXmlDep).value
    Compile / unmanagedSourceDirectories += {
      val base = (Compile / scalaSource).value.getParentFile
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 =>
          base / s"scala-2.13+"
        case _ =>
          base / s"scala-2.13-"
      }
    }
    Compile / unmanagedResources += (baseDirectory map { _ / "LICENSE" }).value
    mimaPreviousArtifacts := {
      val versions = Seq("0.5.0")
      val crossVersion = if (crossPaths.value) CrossVersion.binary else CrossVersion.disabled
      versions.map(v => organization.value % moduleName.value % v cross crossVersion).toSet
    },
  })
  .settings(
    relaxOldScala,
    Fmpp.templateSettings
  )

lazy val treeExample = (project in (file("examples") / "bt"))
  .dependsOn(core)
  .settings(nocomma {
    name := "SBinary Tree Example"
    publish / skip := true
    mimaPreviousArtifacts := Set.empty
  })
  .settings(relaxOldScala)

def relaxOldScala: Seq[Setting[_]] = Seq(
  scalacOptions := {
    val old = scalacOptions.value
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 12 =>
        old filterNot Set(
          "-Xfatal-warnings",
          "-Ywarn-unused-import",
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
