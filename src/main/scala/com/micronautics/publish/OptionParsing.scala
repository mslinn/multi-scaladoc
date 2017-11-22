package com.micronautics.publish

import buildInfo.BuildInfo
import scopt.OptionParser

trait OptionParsing {
  val parser: OptionParser[Config] = new scopt.OptionParser[Config]("bin/run") {
    head("Scaladoc publisher for multi-project SBT builds", BuildInfo.version)

    opt[String]('c', "copyright").action { (value, config) =>
      config.copy(copyright = value)
    }.text("Scaladoc footer")

    opt[Boolean]('r', "dryRun").action { (value, config) =>
      config.copy(dryRun = value)
    }.text("Stubs out 'git commit' and displays the command line that would be run instead, along with the output of 'git status'")

    opt[String]('n', "gitHubName").action { (value, config) =>
      config.copy(gitHubName = Some(value))
    }.required.text("Github project ID for project to be documented")

    opt[String]('u', "gitRemoteOriginUrl").action { (value, config) =>
      config.copy(gitRemoteOriginUrl = Some(value))
    }.required.text("Github project url for project to be documented")

    opt[Boolean]('k', "keepAfterUse").action { (value, config) =>
      config.copy(keepAfterUse = value)
    }.text("Keep the GhPages temporary directory when the program ends")

    opt[Boolean]('o', "overWriteIndex").action { (value, config) =>
      config.copy(overWriteIndex = value)
    }.text(s"Do not preserve any pre-existing index.html in the Scaladoc root")

    opt[String]('s', "subProjectNames").action { (value, config) =>
      config.copy(subProjectNames = value.split(",").toList)
    }.required.text(s"Comma-delimited names of subprojects to generate Scaladoc for")
  }
}
