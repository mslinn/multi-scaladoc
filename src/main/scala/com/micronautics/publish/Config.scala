package com.micronautics.publish

object Config {
  val default: Config = Config()
}

/** @param copyright Scaladoc footer
  * @param dryRun Show the commands that would be run
  * @param gitHubName Github ID for project
  * @param keepAfterUse do not remove the GhPages temporary directory when the program ends
  * @param preserveIndex Preserve any pre-existing index.html file in the Scaladoc root
  * @param subProjectNames Names of subprojects to generate Scaladoc for; delimited by commas */
case class Config(
  copyright: String = "&nbsp;",
  dryRun: Boolean = false,
  gitHubName: Option[String] = None,
  gitRemoteOriginUrl: Option[String] = None,
  keepAfterUse: Boolean = false,
  preserveIndex: Boolean = false,
  subProjectNames: List[String] = Nil
)
