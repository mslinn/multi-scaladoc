package com.micronautics.publish

import java.util.Date

case class GitStatistics(subProject: SubProject) {
  import scala.sys.process._

  /** @return List of contributors for repository */
  def contributors(implicit commandLine: CommandLine): List[String] = {
    import commandLine.cd
    cd(subProject.srcDir)
    val result: ProcessBuilder = "git log --format='%aN'" #| "sort -u"
    result.!!.trim.split("\n").map(_.trim).toList
  }

  /** todo Date format is 1 Jan, 2013
    * todo parse return value*/
  def userLogsFor(name: String, date: Date)
                 (implicit commandLine: CommandLine): String = {
    import commandLine.cd
    cd(subProject.srcDir)
    s"""git log  --shortstat --author="$name" --since="${ date }"""".!!.trim
  }

  /** @return List of (commit count, user name) tuples */
  def userCommitTotals(implicit commandLine: CommandLine): List[(Int, String)] = {
    import commandLine.cd
    cd(subProject.srcDir)
    // Is this better? git shortlog -sn --no-merges
    val lines: Array[String] = "git shortlog -sn".!!
      .trim
      .split("\n")
      .map(_.trim)

    val result: Array[(Int, String)] =
      for { line <- lines } yield {
        line.split(" ")  match {
          case Array(head: String, tail @ _*) => (head.toInt, tail.mkString(" "))
        }
      }

    result.toList
  }
}
