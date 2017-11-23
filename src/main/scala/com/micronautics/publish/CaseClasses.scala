package com.micronautics.publish

import java.io.File

object ScalaCompiler {
  lazy val fullVersion: String = scala.util.Properties.versionNumberString
  lazy val majorMinorVersion: String = fullVersion.split(".").take(2).mkString(".")
}

case class SubProject(baseDirectory: File, name: String)
