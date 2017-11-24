package com.micronautics.publish

import java.nio.file.Path
import com.micronautics.publish.Documenter.log
import org.apache.commons.io.FileUtils
import org.slf4j.Logger

object GhPages {
  val ghPagesBranchName = "gh-pages"
}

/** @param root directory to place the contents of GhPages */
case class GhPages(
  root: Path
)(implicit config: Config, log: Logger) {
  import com.micronautics.publish.GhPages._

  if (!config.keepAfterUse)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = deleteTempDir()
    })

  /** Create common root for Scaladoc for every SBT sub-project, and return the [[Path]] */
  protected[publish] lazy val apisLatestDir: Path = Documenter.apisLatestDir(root)


  /** Directory root to hold Scaladoc for the given SBT sub-project; does not create the [[Path]];
    * this is done when the Scaladoc is generated. */
  @inline protected[publish] def apiDirFor(subProject: SubProject): Path =
    apisLatestDir.resolve(subProject.name)

  @inline protected[publish] def branchExistsLocally(branchName: String = ghPagesBranchName)
                                                    (implicit commandLine: CommandLine): Boolean =
    commandLine.run(root, "git", "show-ref", s"refs/heads/$branchName").nonEmpty

  @inline protected[publish] def branchExistsRemotely(branchName: String = ghPagesBranchName)
                                                     (implicit commandLine: CommandLine): Boolean =
    commandLine
      .run(root, "git", "rev-parse", "--verify", "--no-color", branchName)
      .split(",")
      .contains(ghPagesBranchName)

  /** Clones the `gh-pages` branch into `ghPages/` */
  protected[publish] def gitClone()(implicit commandLine: CommandLine): Unit = {
    import commandLine.run
    config.gitRemoteOriginUrl.map { remoteUrl =>
      run(root.getParent, "git", "clone", "--depth", "1", "-b", ghPagesBranchName, remoteUrl, "ghPages")
      FileUtils.forceMkdir(apisLatestDir.toFile)
      commandLine.lastResult
    }.orElse(throw new Exception("Error: config.gitRemoteOriginUrl was not specified"))
  }

  protected[publish] def createGhPagesBranch()(implicit
    commandLine: CommandLine
  ): Unit = {
    import commandLine.run

    config.gitRemoteOriginUrl.foreach { url =>
      run(root, "git", "clone", url, root.toFile.getName) // todo try "--depth", "1",
    }

    // Remove git history and content
    run(root, "git", "checkout", "--orphan", ghPagesBranchName)
    Nuke.removeUnder(root)

    // Create the gh-pages branch and push it
    run(root, "git", "commit", "--allow-empty", "-m", s"Initialize $ghPagesBranchName branch")
    run(root, "git", "push", "origin", ghPagesBranchName)

    Nuke.remove(root) // All done
  }

  /** Delete any previous Scaladoc while keeping top 3 directories (does not mess with top-level contents). */
  @inline protected[publish] def deleteScaladoc(): Unit = Nuke.removeUnder(apisLatestDir)

  @inline protected[publish] def deleteTempDir(): Unit =
    try {
      Nuke.remove(root)
    } catch {
      case e: Exception => e.printStackTrace()
    }

  protected[publish] def ghPagesBranchExists(implicit commandLine: CommandLine): Boolean = {
    import commandLine.run
    branchExistsLocally(ghPagesBranchName) || {
      if (branchExistsRemotely(ghPagesBranchName)) {
        run(root, "git", "checkout", "--track", s"origin/$ghPagesBranchName")
        branchExistsLocally(ghPagesBranchName)
      } else false
    }
  }

  /** 1) Fetches or creates gh-pages branch if it is not already local
    * 2) Creates any directories that might be needed
    * 3) Just retains ghPages/latest/api
    * 4) Creates new 3rd level directories to hold sbt subproject Scaladoc  */
  protected[publish] def setupGhPages()(implicit commandLine: CommandLine): Unit = {
    try {
      gitClone()
      if (ghPagesBranchExists) deleteScaladoc()
      else createGhPagesBranch()
    } catch {
      case e: Exception =>
        log.error(e.getMessage)
        System.exit(0)
    }
    ()
  }
}
