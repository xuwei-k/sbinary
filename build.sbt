lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.11.3"
def scalaXmlDep(scalaV: String): List[ModuleID] =
  if(scalaV.startsWith("2.11.") || scalaV.startsWith("2.12.")) List("org.scala-lang.modules" %% "scala-xml" % "1.0.2")
  else Nil

lazy val root = (project in file(".")).
  aggregate(core, treeExample).
  settings(
    inThisBuild(Seq(
      organization := "org.scala-sbt",
      organizationHomepage := Some(url("http://scala-sbt.org/")),
      homepage := Some(url("https://github.com/sbt/sbinary")),
      version := "0.4.3",
      scalaVersion := "2.10.6",
      crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-M3"),
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
    Fmpp.templateSettings,
    libraryDependencies += scalacheck % Test,
    libraryDependencies <++= scalaVersion(scalaXmlDep),
    unmanagedResources in Compile <+= baseDirectory map { _ / "LICENSE" }
  )

lazy val treeExample = (project in (file("examples") / "bt")).
  dependsOn(core).
  settings(
    name := "SBinary Tree Example",
    publish := (),
    publishLocal := ()
  )
