package com.micronautics.publish

object Config {
  val default: Config = Config()

  def fromEnvVars: Config =
    default
      .copy(copyright          = sys.env.getOrElse("SCALADOC_COPYRIGHT", default.copyright))
      .copy(dryRun             = sys.env.get("SCALADOC_DRYRUN").map(_.toBoolean).getOrElse(default.dryRun))
      .copy(gitHubName         = sys.env.get("SCALADOC_GITHUB_NAME").orElse(default.gitHubName))
      .copy(gitRemoteOriginUrl = sys.env.get("SCALADOC_GIT_URL").orElse(default.gitRemoteOriginUrl))
      .copy(keepAfterUse       = sys.env.get("SCALADOC_KEEP").map(_.toBoolean).getOrElse(default.keepAfterUse))
      .copy(preserveIndex      = sys.env.get("SCALADOC_PRESERVE_INDEX").map(_.toBoolean).getOrElse(default.preserveIndex))
      .copy(subProjectNames    = sys.env.get("SCALADOC_SUB_PROJECT_NAMES").map(_.split(",").toList).getOrElse(default.subProjectNames))
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
