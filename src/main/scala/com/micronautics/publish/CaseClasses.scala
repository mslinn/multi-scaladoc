package com.micronautics.publish

import java.io.File
import java.util.Date

object ScalaCompiler {
  lazy val fullVersion: String = scala.util.Properties.versionNumberString
  lazy val majorMinorVersion: String = fullVersion.split(".").take(2).mkString(".")
}

case class SubProject(apiDir: File, name: String, srcDir: File) {
  def apiDirExists: Boolean = apiDir.exists

  def srcDirExists: Boolean = srcDir.exists

  lazy val gitStatistics = GitStatistics(this)
}
