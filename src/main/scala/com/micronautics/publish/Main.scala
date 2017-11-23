package com.micronautics.publish

import java.io.File
import buildInfo.BuildInfo
import org.slf4j.Logger

/** Utility for creating combined Scaladoc for an SBT multi-project */
object Main extends App with OptionParsing {
  implicit val commandLine: CommandLine = new CommandLine()

  implicit val logger: Logger = org.slf4j.LoggerFactory.getLogger("pub")

  parser.parse(args, Config.default) match {
     case Some(config) => doIt(config)

     case None =>
       // arguments are bad, error message will have been displayed
   }

  def doIt(implicit config: Config): Unit = {
    implicit val project: Project = Project(
      name    = BuildInfo.name,
      version = BuildInfo.version
    )

    // subprojects to document; others are ignored (such as this one)
    val subprojects: List[SubProject] =
      config
        .subProjectNames
        .map { subProjectName => new SubProject(subProjectName, new File(subProjectName).getAbsoluteFile) }

    new Documenter(subprojects).publish()
  }

//  lazy val gitRemoteOriginUrl: String = commandLine.run("git config --get remote.origin.url")
}
