package com.micronautics.publish

import java.io.File
import java.nio.file.{Files, Path}
import com.micronautics.publish.Documenter.file
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.event.Level.DEBUG

object GhPages {
  val ghPagesBranchName = "gh-pages"
}

/** @param deleteAfterUse Remove the temporary directory holding the GhPages content when the JVM shuts down
  * @param root directory to place the contents of GhPages */
case class GhPages(
  deleteAfterUse: Boolean = true,
  root: Path = Files.createTempDirectory("scaladoc").toAbsolutePath
)(implicit config: Config, log: Logger) {
  import GhPages._

  if (deleteAfterUse)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = deleteTempDir()
    })

  /** Create common root for Scaladoc for every SBT sub-project, and return the [[Path]] */
  lazy val apisRoot: Path = {
    val dir = ghPagesRoot.resolve("latest/api/").toAbsolutePath
    FileUtils.forceMkdir(dir.toFile)
    dir
  }

  lazy val ghPagesRoot: Path = {
    val dir = root.resolve("ghPages").toAbsolutePath
    FileUtils.forceMkdir(dir.toFile)
    dir
  }

  /** Root for Scaladoc for the given SBT sub-project; does not create the [[Path]];
    * this is done by Scaladoc when it runs (todo or is it?) */
  def apiRootFor(subProject: SubProject): Path = apisRoot.resolve(subProject.name).toAbsolutePath

  protected[publish] def checkoutOrClone(gitWorkPath: Path)
                                        (implicit commandLine: CommandLine, project: Project, subProject: SubProject): Unit = {
    import commandLine.run
    run(apiRootFor(subProject), "git", "clone", "--depth", "1", "-b", ghPagesBranchName, s"{ config.gitRemoteOriginUrl }.git")
    file(project.name).renameTo(file(subProject.baseDirectory.getName))
  }


  def branchExistsLocally(branchName: String = ghPagesBranchName)
                         (implicit commandLine: CommandLine): Boolean =
    commandLine.run("git", "show-ref", s"refs/heads/$branchName").nonEmpty

  def branchExistsRemotely(branchName: String = ghPagesBranchName)
                          (implicit commandLine: CommandLine): Boolean =
    commandLine
      .run(root, "git", "rev-parse", "--verify", "--no-color", branchName)
      .split(",")
      .contains(ghPagesBranchName)

  def createGhPagesBranch()(implicit commandLine: CommandLine, project: Project, subProject: SubProject): Unit = {
    import commandLine.run

    val repoDir: File = new File(root.toFile, "repo")
    config.gitRemoteOriginUrl.foreach { url =>
      run(root, "git", "clone", url, repoDir.getName) // todo try "--depth", "1",
    }

    // Remove git history and content
    run(repoDir, "git", "checkout", "--orphan", ghPagesBranchName)
    Nuke.removeUnder(repoDir)

    // Create the gh-pages branch and push it
    run(repoDir, "git", "commit", "--allow-empty", "-m", s"Initialize $ghPagesBranchName branch")
    run(repoDir, "git", "push", "origin", ghPagesBranchName)

    Nuke.remove(repoDir.toPath) // All done
  }

  /** Delete any previous Scaladoc while keeping top 3 directories (does not mess with top-level contents). */
  def deleteScaladoc(): Unit = Nuke.removeUnder(apisRoot.toFile)

  def deleteTempDir(): Unit =
    try {
      Nuke.remove(root)
    } catch {
      case e: Exception => e.printStackTrace()
    }

  def ghPagesBranchExists(implicit commandLine: CommandLine): Boolean = {
    import commandLine.run
    branchExistsLocally(ghPagesBranchName) || {
      if (branchExistsRemotely(ghPagesBranchName)) {
        run(root, "git", "checkout", "--track", s"origin/$ghPagesBranchName")
        branchExistsLocally(ghPagesBranchName)
      } else false
    }
  }

  @inline protected[publish] def dumpDirs(gitWorkPath: Path, subProject: SubProject, gitGit: Path): Unit =
    log.debug(s"""baseDirectory    = ${ subProject.baseDirectory.getAbsolutePath }
                 |cwd              = ${ sys.props("user.dir") }
                 |ghPages.root     = ${ root }
                 |ghPages.apiRoots = ${ apisRoot }
                 |gitWorkPath      = ${ gitWorkPath }
                 |gitGit           = $gitGit
                 |""".stripMargin)
}
