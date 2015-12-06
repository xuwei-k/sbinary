lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.11.3"
def scalaXmlDep(scalaV: String): List[ModuleID] =
  if(scalaV.startsWith("2.11.") || scalaV.startsWith("2.12.")) List("org.scala-lang.modules" %% "scala-xml" % "1.0.2")
  else Nil

lazy val root = (project in file(".")).
  aggregate(core, treeExample).
  settings(
    inThisBuild(Seq(
      organization := "org.scala-tools.sbinary",
      version := "0.4.3-SNAPSHOT",
      scalaVersion := "2.10.6"
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
