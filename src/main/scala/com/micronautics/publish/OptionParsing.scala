package com.micronautics.publish

import java.util.StringTokenizer
import buildInfo.BuildInfo
import scopt.OptionParser

trait OptionParsing {
  def overrides(envVar: String): String =
    s"This command-line parameter overrides the $envVar environment variable."

  def wrap(text: String, lineWidth: Int = 80, leftPad: Int = 8): String = {
    var spaceLeft = lineWidth
    val result = new StringBuffer
    val tokenizer = new StringTokenizer(text)
    while (tokenizer.hasMoreTokens) {
      val word: String = tokenizer.nextToken
      if ((word.length + 1) > spaceLeft) {
        result.append("\n" + (" " * leftPad) + word + " ")
        spaceLeft = lineWidth - word.length - leftPad
      } else {
        result.append(word + " ")
        spaceLeft -= (word.length + 1)
      }
    }
    result.toString + "\n"
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
