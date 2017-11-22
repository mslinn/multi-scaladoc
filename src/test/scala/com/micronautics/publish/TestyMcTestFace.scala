package com.micronautics.publish

import java.io.File
import java.nio.file.{Files, Path}
import buildInfo.BuildInfo
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.slf4j.Logger

@RunWith(classOf[JUnitRunner])
class TestyMcTestFace extends WordSpec with MustMatchers {
  implicit val logger: Logger = org.slf4j.LoggerFactory.getLogger("pub")

  implicit val config: Config =
    Config
      .default
      .copy(
        gitHubName = Some("mslinn"),
        gitRemoteOriginUrl = Some("git@github.com:mslinn/web3j-scala.git"),
        subProjectNames = List("root", "demo")
      )
      //.copy(deleteAfterUse = false)

  implicit protected [publish] val commandLine: CommandLine = new CommandLine

  lazy val gitHubUserUrl: String = commandLine.run("git config --get remote.origin.url")

  implicit val project: Project =
    Project(
      name    = BuildInfo.gitRepoName,
      version = BuildInfo.version
    )

  // subprojects to document; others are ignored (such as this one)
  val subprojects: List[SubProject] =
    List("root", "demo")
      .map(x => new SubProject(x, new File(x).getAbsoluteFile))

  val documenter = new Documenter(subprojects)
  val subProjects: List[SubProject] = documenter.subProjects
  val ghPages: GhPages = documenter.ghPages

  "Nuke" should {
    "work" in {
      val root: Path = Files.createTempDirectory("ghPages")

      val abc: File = root.resolve("a/b/c").toFile
      abc.mkdirs

      val ab: File = abc.getParentFile
      val a: File = ab.getParentFile

      abc.listFiles.length shouldBe 0

      ab.listFiles.length shouldBe 1
      Nuke.removeUnder(ab)
      ab.listFiles.length shouldBe 0

      a.listFiles.length shouldBe 1
      Nuke.remove(ab.toPath)
      a.listFiles.length shouldBe 0
      Nuke.remove(ab.toPath)
      a.listFiles.length shouldBe 0

      Nuke.remove(root)
    }
  }

  "GhPages subprojects" should {
    "work" in {
      ghPages.apisRoot mustBe ghPages.root.resolve("latest/api")
      subProjects.find(_.name=="root").map(ghPages.apiRootFor).value mustBe ghPages.root.resolve("latest/api/root")
    }
  }

  "GhPages branch creation" should {
    "work" ignore { // todo currently tests with a live project, need a dummy project for testing
      val root: Path = Files.createTempDirectory("ghPages")
      val repoDir = new File(root.toFile, "repo")
      val ghPagesBranchName = "gh-pages"
      config.gitRemoteOriginUrl.foreach { url =>
        commandLine.run(root, "git", "clone", url, repoDir.getName) // todo try "--depth", "1",
      }
      commandLine.run(repoDir, "git", "checkout", "--orphan", ghPagesBranchName)
      Nuke.removeUnderExceptGit(repoDir)
      assert(repoDir.toPath.resolve(".git").toFile.exists, ".git directory got clobbered")
      repoDir.list.length shouldBe 1

      // Establish the branch existence
      commandLine.run(repoDir, "git", "commit", "--allow-empty", "-m", s"Initialized $ghPagesBranchName branch")
      //commandLine.run(repoDir, "git", "push", "origin", ghPagesBranchName) // this is a live test, don't want to affect the git project
      Nuke.remove(root)
    }
  }

  "Setup" should {
    "work" in {
      documenter.setup()
      val rootFileNames: Array[String] = ghPages.root.toFile.listFiles.map(_.getName)
      logger.info(s"ghPages.root (${ ghPages.root }) contains ${ rootFileNames.mkString(", ") }")
      rootFileNames mustBe Array("latest")
      ghPages.deleteScaladoc()
      ghPages.root.resolve("latest/api").toFile.listFiles.length mustBe 0
    }
  }

  "RunScaladoc" should {
    "work" ignore { // fails under travis
      subProjects.foreach(documenter.runScaladoc)

      ghPages.root.resolve("latest/api/demo").toFile.listFiles.length must be > 0
      ghPages.root.resolve("latest/api/root").toFile.listFiles.length must be > 0

      /*import java.awt.Desktop
      import java.net.URI

      if (Desktop.isDesktopSupported) {
        val demo: Path = ghPages.root.resolve("latest/api/demo")
        val uri: URI = new URI(s"file://$demo")
        Desktop.getDesktop.browse(uri)
      }*/
    }
  }
}
