package com.micronautics.publish

import java.io.File
import java.nio.file.{Files, Path}
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
      //.copy(keepAfterUse = true)

  implicit protected [publish] val commandLine: CommandLine = new CommandLine

  lazy val gitHubUserUrl: String = commandLine.run("git config --get remote.origin.url")

  // subprojects to document; others are ignored (such as this one)
  val subProjects: List[SubProject] =
    List("root", "demo")
      .map(name => SubProject(new File(name).getAbsoluteFile, name))

  val documenter = new Documenter(
    root = Documenter.temporaryDirectory,
    subProjects = subProjects
  )
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
      ghPages.apisRoot mustBe ghPages.ghPagesRoot.resolve("latest/api")
      subProjects.find(_.name=="root").map(ghPages.apiRootFor).value mustBe ghPages.ghPagesRoot.resolve("latest/api/root")
    }
  }

  "GhPages branch creation" should {
    "work" ignore { // todo currently tests with a live project, need a dummy project for testing
      val root: Path = Files.createTempDirectory("ghPages")
      val ghPagesDir = new File(root.toFile, "ghPages")
      val ghPagesBranchName = "gh-pages"
      config.gitRemoteOriginUrl.foreach { url =>
        commandLine.run(root, "git", "clone", url, ghPagesDir.getName) // todo try "--depth", "1",
      }
      commandLine.run(ghPagesDir, "git", "checkout", "--orphan", ghPagesBranchName)
      Nuke.removeUnderExceptGit(ghPagesDir)
      assert(ghPagesDir.toPath.resolve(".git").toFile.exists, ".git directory got clobbered")
      ghPagesDir.list.length shouldBe 1

      // Establish the branch existence
      commandLine.run(ghPagesDir, "git", "commit", "--allow-empty", "-m", s"Initialized $ghPagesBranchName branch")
      //commandLine.run(repoDir, "git", "push", "origin", ghPagesBranchName) // this is a live test, don't want to affect the git project
      Nuke.remove(root)
    }
  }

  "Setup" should {
    "work" in {
      documenter.setupSubProject(subProjects.head)
      val ghPagesRootFileNames: Array[String] = ghPages.ghPagesRoot.toFile.listFiles.map(_.getName)
      logger.info(s"ghPages.ghPagesRoot (${ ghPages.ghPagesRoot }) contains ${ ghPagesRootFileNames.mkString(", ") }")
      ghPagesRootFileNames mustBe Array("latest")
      ghPages.deleteScaladoc()
      ghPages.ghPagesRoot.resolve("latest/api").toFile.listFiles.length mustBe 0
    }
  }

  "RunScaladoc" should {
    "work" ignore { // fails under travis
      subProjects.foreach(documenter.createScaladocFor)

      ghPages.ghPagesRoot.resolve("latest/api/demo").toFile.listFiles.length must be > 0
      ghPages.ghPagesRoot.resolve("latest/api/root").toFile.listFiles.length must be > 0

      /*import java.awt.Desktop
      import java.net.URI

      if (Desktop.isDesktopSupported) {
        val demo: Path = ghPages.ghPagesRoot.resolve("latest/api/demo")
        val uri: URI = new URI(s"file://$demo")
        Desktop.getDesktop.browse(uri)
      }*/
    }
  }
}
