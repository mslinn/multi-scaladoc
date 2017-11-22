package com.micronautics.publish

import java.io.File
import java.nio.file.{Path, Paths}
import org.apache.commons.io.FileUtils
import org.slf4j.Logger

object Nuke {
  implicit def pathToFile(path: Path): File = path.toFile

  /** Adapted from https://stackoverflow.com/a/45703150/553865 */
  @inline def remove(root: Path, deleteRoot: Boolean = true)
            (implicit log: Logger): Unit = {
    log.debug(
      if (deleteRoot) s"Nuking $root"
      else s"Clearing files and directories under $root (${ root.list.mkString(", ") })"
    )

    if (deleteRoot) FileUtils.deleteDirectory(root)
    else FileUtils.cleanDirectory(root)
  }

  @inline def remove(string: String)
               (implicit log: Logger): Unit = remove(Paths.get(string))

  @inline def remove(file: File)
               (implicit log: Logger): Unit = remove(file.toPath)

  @inline def removeUnder(string: String)
                 (implicit log: Logger): Unit = remove(Paths.get(string), deleteRoot=false)

  @inline def removeUnder(file: File)
                 (implicit log: Logger): Unit = remove(file.toPath, deleteRoot=false)

  @inline def removeUnderExceptGit(file: File)
                                  (implicit log: Logger): Unit =
    file
      .list((dir: File, name: String) => name == ".git" && new File(dir, name).isDirectory)
      .foreach(remove)


  @inline protected def relativize(parent: Path, list: Array[String]): Array[String] =
    list.map(x => parent.relativize(Paths.get(x)).toString)
}
