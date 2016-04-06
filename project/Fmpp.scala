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
    fullClasspath in fmppConfig <<= update map { _ select configurationFilter(fmppConfig.name) map Attributed.blank }
  )

  def fmppConfig(c: Configuration): Seq[Setting[_]] = inConfig(c)(Seq(
    sourceGenerators <+= fmpp,
    fmpp <<= fmppTask,
    scalaSource <<= (baseDirectory, configuration) { (base,c) => base / (Defaults.prefix(c.name) + "src") },
    mappings in packageSrc <<= (managedSources, sourceManaged) map { (srcs, base) => srcs x relativeTo(base) },
    sources <<= managedSources
  ))
  lazy val fmppTask =
    (fullClasspath in fmppConfig, runner in fmpp, unmanagedSources, scalaSource, sourceManaged, fmppOptions, streams) map { (cp, r, sources, srcRoot, output, args, s) =>
      IO.delete(output)
      val arguments = "-U" +: "all" +: "-S" +: srcRoot.getAbsolutePath +: "-O" +: output.getAbsolutePath +: (args ++ sources.getPaths)
      toError(r.run("fmpp.tools.CommandLine", cp.files, arguments, s.log))
      (output ** "*.scala").get
    }
}
