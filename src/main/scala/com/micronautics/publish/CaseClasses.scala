package com.micronautics.publish

import java.io.File

/** @param name GitHub repo name for this SBT project
  * @param gitRemoteOriginUrl taken from `.git/config`
  * @param version Git release version of this SBT project */
case class Project(
  baseDirectory: File = new File(sys.props("user.dir")).getAbsoluteFile,
  name: String,
  version: String
) {
  //require(io.Source.fromURL(gitRemoteOriginUrl).mkString.trim.nonEmpty, s"$gitRemoteOriginUrl does not exist")
}

object ScalaCompiler {
  lazy val fullVersion: String = scala.util.Properties.versionNumberString
  lazy val majorMinorVersion: String = fullVersion.split(".").take(2).mkString(".")
}

case class SubProject(baseDirectory: File, name: String) {
//  lazy val crossTarget: File = new File(s"target/scala-${ ScalaCompiler.majorMinorVersion }")
}
