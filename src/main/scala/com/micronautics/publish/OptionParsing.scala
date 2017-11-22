package com.micronautics.publish

import buildInfo.BuildInfo
import scopt.OptionParser

trait OptionParsing {
  val parser: OptionParser[Config] = new scopt.OptionParser[Config]("bin/doc") {
    head("Scaladoc publisher for multi-project SBT builds", BuildInfo.version)

    opt[Boolean]('a', "autoCheckIn").action { (value, config) =>
      config.copy(autoCheckIn = value)
    }.text("Stop program if this flag is not specified and any files need to be committed or pushed")

    opt[String]('c', "copyright").action { (value, config) =>
      config.copy(copyright = value)
    }.text("Scaladoc footer")

    opt[Boolean]('d', "deleteAfterUse").action { (value, config) =>
      config.copy(deleteAfterUse = value)
    }.text("Remove the GhPages temporary directory when the program ends")

    opt[String]('n', "gitHubName").action { (value, config) =>
      config.copy(gitHubName = value)
    }.required.text("Github project ID for project to be documented")

    opt[Boolean]('o', "overWriteIndex").action { (value, config) =>
      config.copy(overWriteIndex = value)
    }.text(s"Do not preserve any pre-existing index.html in the Scaladoc root")

    opt[Boolean]('r', "dryRun").action { (value, config) =>
      config.copy(dryRun = value)
    }.text("Show the commands that would be run")

    opt[String]('s', "subProjectNames").action { (value, config) =>
      config.copy(subProjectNames = value.split(",").toList)
    }.required.text(s"Comma-delimited names of subprojects to generate Scaladoc for")
  }
}
