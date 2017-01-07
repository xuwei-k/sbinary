lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4"
def scalaXmlDep(scalaV: String): List[ModuleID] =
  if(scalaV.startsWith("2.11.") || scalaV.startsWith("2.12.")) List("org.scala-lang.modules" %% "scala-xml" % "1.0.5")
  else Nil

def relaxNon212: Seq[Setting[_]] = Seq(
    scalacOptions := {
      val old = scalacOptions.value
      scalaBinaryVersion.value match {
        case "2.12" => old
        case _      => old filterNot (Set("-Xfatal-warnings", "-deprecation", "-Ywarn-unused", "-Ywarn-unused-import").apply _)
      }
    }
  )

lazy val root = (project in file(".")).
  aggregate(core, treeExample).
  settings(
    inThisBuild(Seq(
      organization := "org.scala-sbt",
      organizationHomepage := Some(url("http://scala-sbt.org/")),
      homepage := Some(url("https://github.com/sbt/sbinary")),
      version := "0.4.4-SNAPSHOT",
      scalaVersion := "2.12.1",
      crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1"),
      bintrayPackage := "sbinary",
      developers := List(
        Developer("drmaciver", "David R. MacIver", "@drmaciver", url("https://github.com/DRMacIver")),
        Developer("harrah", "Mark Harrah", "@harrah", url("https://github.com/harrah")),
        Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
      ),
      description := "Library for describing binary formats for Scala types",
      licenses := Seq("MIT" -> new URL("https://github.com/sbt/sbinary/blob/master/LICENSE")),
      scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbinary"), "git@github.com:sbt/sbinary.git"))
    )),
    name := "SBinary Parent",
    publish := (),
    publishLocal := ()
  )

lazy val core = (project in file("core")).
  settings(
    name := "SBinary",
    relaxNon212,
    Fmpp.templateSettings,
    libraryDependencies += scalacheck % Test,
    libraryDependencies ++= scalaVersion(scalaXmlDep).value,
    unmanagedResources in Compile += (baseDirectory map { _ / "LICENSE" } ).value
  )

lazy val treeExample = (project in (file("examples") / "bt")).
  dependsOn(core).
  settings(
    name := "SBinary Tree Example",
    relaxNon212,
    publish := (),
    publishLocal := ()
  )
