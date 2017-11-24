package com.micronautics.publish

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import java.util.UUID
import org.apache.commons.io.FileUtils
import org.slf4j.event.Level._
import FSMLike._

object Documenter {
  @inline def file(name: String): File = new File(name)

  implicit val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("pub")

  @inline def temporaryDirectory: Path = Files.createTempDirectory("scaladoc").toAbsolutePath

  val states: Map[UUID, FSMLike[_]] =
    List(
//      (stop.id, stop)
    ).toMap

  val fsm = FiniteStateMachine(states)
}

case class Documenter(
  root: Path,
  subProjects: List[SubProject]
)(implicit
  commandLine: CommandLine,
  config: Config
) {
  import com.micronautics.publish.Documenter._
  import commandLine.run

  config.gitRemoteOriginUrl.map { url =>
   run(root, "git", "clone", "--depth", "1", url, "master")
  }.getOrElse {
    log.error("Error: config.gitRemoteOriginUrl was not specified")
    System.exit(0)
    ""
  }

  protected[publish] lazy val ghPages: GhPages = GhPages(root.resolve("ghPages"))

  /** Path where the master branch of the project is cloned into */
  protected[publish] lazy val masterDir: Path = root.resolve("master")

  lazy val name: String = config.gitRemoteOriginUrl.map { url =>
    val last = url.split("/").last
    last.substring(0, last.indexOf(".git"))
  }.mkString

  lazy val version: String = run(masterDir, "sbt", "-no-colors", "show version").split("\n").last.split(" ").last

  def publish(): Unit = {
    try {
      subProjects.foreach(subProject => setupSubProject(subProject))
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


  protected[publish] def createScaladocFor(subProject: SubProject): Unit = {
    log.info(s"Creating Scaladoc for ${ subProject.name }.")

    val outputDirectory: Path = ghPages.apiRootFor(subProject)

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
      sourcePath = new File(subProject.baseDirectory, "src/main/scala").getAbsolutePath,
      sourceUrl = sourceUrl,
      title = name,
      version = version
    ).run(subProject.baseDirectory, commandLine)
    ()
  }

  protected[publish] def gitAddCommitPush(message: LogMessage = LogMessage.empty)
                                         (implicit subProject: SubProject): Unit = {
    FileUtils.forceMkdir(ghPages.apiRootFor(subProject).toFile)
    run(ghPages.root, "git add --all")(message, log)
    run(ghPages.root, "git commit -m -")
    run(ghPages.root, "git push origin HEAD")
  }

  /** 1) Fetches or creates gh-pages branch if it is not already local
    * 2) Creates any directories that might be needed
    * 3) Just retains the top 2 directories
    * 4) Creates new 3rd level directories to hold sbt subproject Scaladoc
    * 5) Commits the branch */
  protected[publish] def setupSubProject(implicit subProject: SubProject): Unit = {
    try {
      ghPages.clone(ghPages.apiRootFor(subProject))
      if (ghPages.ghPagesBranchExists) ghPages.deleteScaladoc()
      else ghPages.createGhPagesBranch()
      gitAddCommitPush()
    } catch {
      case e: Exception =>
        log.error(e.getMessage)
        System.exit(0)
    }
    ()
  }

  protected def writeIndex(preserveIndex: Boolean = false): Unit = {
    val ghFile = ghPages.ghPagesRoot.toFile
    if (!preserveIndex || ghFile.list.isEmpty) {
      val index: File = new File(ghFile, "index.html")
      val contents: String = subProjects.map { subProject =>
        s"<a href='api/latest/${ subProject.name }/index.html' class='extype'><code>${ subProject.name }</code></a><br/>"
      }.mkString("<p>", "\n", "</p>")

      FileUtils.write(index,
        s"""
           |<!DOCTYPE html >
           |<html>
           |<head>
           |  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
           |  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
           |  <title>$name v$version API</title>
           |  <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
           |  <link href="lib/index.css" media="screen" type="text/css" rel="stylesheet" />
           |  <link href="lib/template.css" media="screen" type="text/css" rel="stylesheet" />
           |</head>
           |<body>
           |<div id="content-scroll-container" style="-webkit-overflow-scrolling: touch;">
           |  <div id="content-container" style="-webkit-overflow-scrolling: touch;">
           |    <div id="subpackage-spacer">
           |      <div id="packages">
           |        $contents
           |      </div>
           |    </div>
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
