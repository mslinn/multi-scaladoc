package com.micronautics.publish

import java.nio.file.{Files, Path}
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
  protected[publish] lazy val apisRoot: Path = {
    val dir = ghPagesRoot.resolve("latest/api/").toAbsolutePath
//    FileUtils.forceMkdir(dir.toFile)
    dir
  }

  /** @return the [[Path]] for the `gh-pages` branch */
  @inline protected[publish] lazy val ghPagesRoot: Path = {
    val dir = root.resolve("ghPages").toAbsolutePath
//    FileUtils.forceMkdir(dir.toFile)
    dir
  }

  /** Root for Scaladoc for the given SBT sub-project; does not create the [[Path]];
    * this is done when the Scaladoc is generated. */
  @inline protected[publish] def apiRootFor(subProject: SubProject): Path = apisRoot.resolve(subProject.name).toAbsolutePath

  @inline protected[publish] def branchExistsLocally(branchName: String = ghPagesBranchName)
                         (implicit commandLine: CommandLine): Boolean =
    commandLine.run(ghPagesRoot, "git", "show-ref", s"refs/heads/$branchName").nonEmpty

  @inline protected[publish] def branchExistsRemotely(branchName: String = ghPagesBranchName)
                          (implicit commandLine: CommandLine): Boolean =
    commandLine
      .run(root, "git", "rev-parse", "--verify", "--no-color", branchName)
      .split(",")
      .contains(ghPagesBranchName)

  /** Clones the `gh-pages` branch into `ghPages/` */
  protected[publish] def clone(gitWorkPath: Path)
                              (implicit commandLine: CommandLine): Unit = {
    import commandLine.run
    config.gitRemoteOriginUrl.map { remoteUrl =>
      run(root, "git", "clone", "--depth", "1", "-b", ghPagesBranchName, remoteUrl, "ghPages")
      FileUtils.forceMkdir(apisRoot.toFile)
      ""
    }.orElse(throw new Exception("Error: config.gitRemoteOriginUrl was not specified"))
  }

  protected[publish] def createGhPagesBranch()(implicit
    commandLine: CommandLine,
    subProject: SubProject
  ): Unit = {
    import commandLine.run

    config.gitRemoteOriginUrl.foreach { url =>
      run(root, "git", "clone", url, ghPagesRoot.toFile.getName) // todo try "--depth", "1",
    }

    // Remove git history and content
    run(ghPagesRoot, "git", "checkout", "--orphan", ghPagesBranchName)
    Nuke.removeUnder(ghPagesRoot)

    // Create the gh-pages branch and push it
    run(ghPagesRoot, "git", "commit", "--allow-empty", "-m", s"Initialize $ghPagesBranchName branch")
    run(ghPagesRoot, "git", "push", "origin", ghPagesBranchName)

    Nuke.remove(ghPagesRoot) // All done
  }

  /** Delete any previous Scaladoc while keeping top 3 directories (does not mess with top-level contents). */
  @inline protected[publish] def deleteScaladoc(): Unit = Nuke.removeUnder(apisRoot)

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
        run(ghPagesRoot, "git", "checkout", "--track", s"origin/$ghPagesBranchName")
        branchExistsLocally(ghPagesBranchName)
      } else false
    }
  }
}
