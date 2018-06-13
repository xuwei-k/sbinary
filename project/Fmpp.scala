import sbt._
import Keys._

object Fmpp {
  /*** Templating **/
  lazy val fmpp = TaskKey[Seq[File]]("fmpp")
  lazy val fmppOptions = SettingKey[Seq[String]]("fmpp-options")
  lazy val fmppConfig = config("fmpp") hide

  lazy val templateSettings = fmppConfig(Test) ++ fmppConfig(Compile) ++ templateBase
  lazy val templateBase = Seq(
    libraryDependencies += "net.sourceforge.fmpp" % "fmpp" % "0.9.14" % fmppConfig.name,
    ivyConfigurations += fmppConfig,
    fmppOptions := "--ignore-temporary-files" :: Nil,
    fullClasspath in fmppConfig := update.value select configurationFilter(fmppConfig.name) map Attributed.blank
  )

  def fmppConfig(c: Configuration): Seq[Setting[_]] = inConfig(c)(Seq(
    sourceGenerators += fmpp,
    fmpp := fmppTask.value,
    mappings in packageSrc ++= managedSources.value pair relativeTo(sourceManaged.value)
  ))
  lazy val fmppTask =
    Def.task {
      val cp = (fullClasspath in fmppConfig).value
      val r = (runner in fmpp).value
      val srcRoot = baseDirectory.value / "src" / "main" / "fmpp"
      val sources = (srcRoot ** "*.scala").get
      val output = sourceManaged.value
      val args = fmppOptions.value
      val s = streams.value
      IO.delete(output)
      val arguments = "-U" +: "all" +: "-S" +: srcRoot.getAbsolutePath +: "-O" +: output.getAbsolutePath +: (args ++ sources.getPaths)
      r.run("fmpp.tools.CommandLine", cp.files, arguments, s.log).foreach(sys.error)
      (output ** "*.scala").get
    }
}
