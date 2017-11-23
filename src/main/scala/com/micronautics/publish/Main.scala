package com.micronautics.publish

import java.io.File
import org.slf4j.Logger

/** Utility for creating combined Scaladoc for an SBT multi-project */
object Main extends App with OptionParsing {
  implicit val commandLine: CommandLine = new CommandLine()

  implicit val logger: Logger = org.slf4j.LoggerFactory.getLogger("pub")

  parser.parse(args, Config.default) match {
     case Some(config) => main(config)

     case None =>
       println()
       parser.showUsage()
       System.exit(-1)
       // arguments are bad, error message will have been displayed
   }

  def main(implicit config: Config): Unit = {
    // subprojects to document; other subprojects are ignored
    val subProjects: List[SubProject] =
      config
        .subProjectNames
        .map { name => SubProject(baseDirectory = new File(name).getAbsoluteFile, name = name) }

    new Documenter(
      root = Documenter.temporaryDirectory,
      subProjects = subProjects
    ).publish()
  }

//  lazy val gitRemoteOriginUrl: String = commandLine.run("git config --get remote.origin.url")
}
