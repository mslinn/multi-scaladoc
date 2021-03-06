package com.micronautics.publish

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import org.apache.commons.io.FileUtils
import org.slf4j.event.Level._

object Documenter {
  implicit val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("pub")

  val rootDir: Path = Documenter.temporaryDirectory


  @inline def file(name: String): File = new File(name)

  def apisLatestDir(ghPagesDir: Path): Path = ghPagesDir.resolve("latest/api/")

  @inline def temporaryDirectory: Path = Files.createTempDirectory("scaladoc")
}

case class Documenter(
  root: Path
)(implicit
  commandLine: CommandLine,
  config: Config
) {
  import com.micronautics.publish.Documenter._
  import commandLine.run

  /* Get a fresh copy of the project to be documented */
  config.gitRemoteOriginUrl.map { url =>
   run(root, "git", "clone", "--depth", "1", url, "master")
  }.getOrElse {
    log.error("Error: config.gitRemoteOriginUrl was not specified")
    System.exit(0)
    ""
  }

  protected[publish] implicit lazy val ghPages: GhPages = GhPages(root.resolve("ghPages"))

  /** Path where the master branch of the project is cloned into */
  protected[publish] lazy val masterDir: Path = root.resolve("master")

  lazy val name: String = config.gitRemoteOriginUrl.map { url =>
    val last = url.split("/").last
    last.substring(0, last.indexOf(".git"))
  }.mkString

  lazy val version: String =
    run(masterDir, "sbt", "-no-colors", "show version")
      .split("\n")
      .map(_.trim)
      .filter(_.nonEmpty)
      .last
      .split(" ")
      .map(_.trim)
      .filter(_.nonEmpty)
      .last

  // Subprojects to document; other subprojects are ignored
  lazy val subProjects: List[SubProject] =
    config
      .subProjectNames
      .map { name =>
        val subProject = SubProject(
          apiDir = apisLatestDir(ghPages.root).resolve(name).toFile,
          name = name,
          srcDir = masterDir.resolve(name).toFile
        )
        if (!subProject.srcDirExists) {
          Console.err.println(
            s"""Error: ${ subProject.srcDir.getAbsolutePath } does not exist.
               |Are you sure that an SBT subproject called '$name' actually exists in this project?
               |Gathering the list of available SBT subprojects in project $name...
               |""".stripMargin)
            val subProjectsFound =
              run(masterDir, "sbt projects")
                .split("In file:")
                .last
                .replace("[info] \t *", "")
                .split("\n")
                .tail
                .map(_.trim)
                .mkString(", ")
          Console.err.println(s"Found these subprojects: $subProjectsFound")
          System.exit(-2)
        }
        subProject
      }

  def publish(): Unit = {
    try {
      ghPages.setupGhPages()
      writeIndex(preserveIndex = config.preserveIndex)

      log.info(s"Making Scaladoc for ${ subProjects.size } SBT subprojects.")
      subProjects.foreach { implicit subProject =>
        createScaladocFor(subProject)
        gitAddCommitPush(LogMessage(INFO, "Uploading Scaladoc to GitHub Pages"))
      }
      if (config.keepAfterUse) LogMessage(INFO, s"The temporary directory at $root  has been preserved.")
    } catch {
      case e: Exception =>
        log.error(e.getMessage)
        System.exit(0)
    }
    ()
  }


  protected[publish] def createScaladocFor(implicit subProject: SubProject): Unit = {
    log.info(s"Creating Scaladoc for ${ subProject.name }.")

    val outputDirectory: Path = ghPages.apiDirFor(subProject)

    val classPath: String =
      run(masterDir, "sbt", "-no-colors", s"; project ${ subProject.name }; export runtime:fullClasspath")
        .split("\n")
        .last

    val sourceUrl: String = config.gitHubName.map { gitHubName =>
      s"https://github.com/$gitHubName/$name/tree/master€{FILE_PATH}.scala"
    }.getOrElse(throw new Exception("Error: config.gitHubName was not specified"))

    Scaladoc(
      classPath = classPath,
      externalDoc = "", // todo figure this out
      footer = config.copyright,
      outputDirectory = outputDirectory,
      sourcePath = subProject.srcDir.getAbsolutePath,
      sourceUrl = sourceUrl,
      title = name,
      version = version
    ).run(subProject.apiDir, commandLine)
    ()
  }

  protected[publish] def gitAddCommitPush(message: LogMessage = LogMessage.empty)
                                         (implicit subProject: SubProject): Unit = {
    FileUtils.forceMkdir(ghPages.apiDirFor(subProject).toFile)
    run(ghPages.root, "git add --all")(message, log)
    run(ghPages.root, "git commit -m -")
    run(ghPages.root, "git push origin HEAD")
  }

  protected def writeIndex(preserveIndex: Boolean = false): Unit = {
    val indexHtml: File = new File(ghPages.root.toFile, "index.html")
    if (!preserveIndex || !indexHtml.exists) {
      val contents: String = subProjects.map { subProject =>
        s"        <li style='margin-left: 1em; line-height: 2em;'><a href='latest/api/${ subProject.name }/index.html'>${ subProject.name }</a><br/></li>"
      }.mkString("\n")

      FileUtils.write(indexHtml,
        s"""<!DOCTYPE html>
           |<html>
           |<head>
           |  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
           |  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
           |  <title>$name v$version APIs</title>
           |  <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
           |  <link href="https://www.scala-lang.org/api/current/lib/index.css" type="text/css" rel="stylesheet" />
           |  <link href="https://www.scala-lang.org/api/current/lib/template.css" type="text/css" rel="stylesheet" />
           |</head>
           |<body>
           |<div id="content-scroll-container" style="-webkit-overflow-scrolling: touch;">
           |  <div id="content-container" style="-webkit-overflow-scrolling: touch;">
           |    <h1 class='signature' id='signature'><span class='symbol'><a href='https://www.github.com/${ config.gitHubName.mkString }/$name'>$name</a></span> v$version APIs</h1>
           |	  <div id='content'>
           |     <p>Not all <code>$name</code> subprojects may be documented. Documented subprojects are:</p>
           | 	  	<ul class='symbol'>
           |$contents
           |	  	</ul>
           |    <p>This documentation was generated by <a href="https://github.com/mslinn/scaladoc"><code>mslinn/scaladoc</code></a>.</p>
           | 	  </div>
           |  </div>
           |</div>
           |</body>
           |</html>
           |""".stripMargin,
        Charset.forName("UTF-8")
      )
    }
  }
}
