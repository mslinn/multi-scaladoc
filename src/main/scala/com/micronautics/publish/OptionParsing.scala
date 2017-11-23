package com.micronautics.publish

import java.util.StringTokenizer
import buildInfo.BuildInfo
import scopt.OptionParser

trait OptionParsing {
  def overrides(envVar: String): String =
    s"This command-line parameter overrides the $envVar environment variable."

  // Taken from https://github.com/scopt/scopt/issues/60
  def wrap(string: String, leftOffset: Int = 8, maxWidth: Int = 80): String = {
    val oneLine = string.lines.mkString(" ").split(' ')
    lazy val indent = " " * leftOffset
    val output = new StringBuilder(oneLine.head)
    var currentLineWidth = oneLine.head.length
    for(chunk <- oneLine.tail) {
      val addedWidth = currentLineWidth + chunk.length + 1
      if (addedWidth > maxWidth){
        output.append(s"\n$indent")
        output.append(chunk)
        currentLineWidth = chunk.length
      } else {
        currentLineWidth = addedWidth
        output.append(' ')
        output.append(chunk)
      }
    }
    output.mkString + "\n"
  }

  val parser: OptionParser[Config] = new scopt.OptionParser[Config]("bin/run") {
    head("Scaladoc publisher for multi-project SBT builds", BuildInfo.version)

    override def renderingMode = scopt.RenderingMode.OneColumn

    opt[String]('c', "copyright").action { (value, config) =>
      config.copy(copyright = value)
    }.text(wrap(s"Scaladoc footer. ${ overrides("SCALADOC_COPYRIGHT") }"))

    help("help").abbr("h").text(wrap("Display this help message"))

    opt[Unit]('k', "keepAfterUse").action { (_, config) =>
      config.copy(keepAfterUse = true)
    }.text(wrap(s"Keep the scaladocXXXX temporary directory when the program ends.\n${ overrides("SCALADOC_KEEP") }"))

    opt[String]('n', "gitHubName").action { (value, config) =>
      config.copy(gitHubName = Some(value))
    }.required.text(wrap(s"Github project ID for project to be documented. ${ overrides("SCALADOC_GITHUB_NAME") }"))

    opt[Unit]('p', "preserveIndex").action { (_, config) =>
      config.copy(preserveIndex = true)
    }.text(wrap("Preserve any pre-existing index.html in the Scaladoc root. " +
           "If this option is not specified, the file is regenerated each time this program runs. " +
            overrides("SCALADOC_PRESERVE_INDEX")))

    opt[Unit]('r', "dryRun").action { (_, config) =>
      config.copy(dryRun = true)
    }.text(wrap("Stubs out 'git commit' and displays the command line that would be run instead, along with the output of 'git status'." +
           overrides("SCALADOC_DRYRUN")))

    opt[String]('s', "subProjectNames").action { (value, config) =>
      config.copy(subProjectNames = value.split(",").toList)
    }.required.text(wrap(s"Comma-delimited names of subprojects to generate Scaladoc for. ${ overrides("SCALADOC_SUB_PROJECT_NAMES") }"))

    opt[String]('u', "gitRemoteOriginUrl").action { (value, config) =>
      config.copy(gitRemoteOriginUrl = Some(value))
    }.required.text(wrap(s"Github project url for project to be documented. ${ overrides("SCALADOC_GIT_URL") }"))
  }
}
