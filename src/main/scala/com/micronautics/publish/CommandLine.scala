package com.micronautics.publish

import org.slf4j.{Logger, LoggerFactory}
import org.slf4j.event.Level
import scala.collection.mutable

object LogMessage {
  implicit val empty: LogMessage = LogMessage(Level.INFO, "")(LoggerFactory.getLogger(""))
}

case class LogMessage(level: Level, message: String)
                     (implicit logger: Logger) {
  lazy val isEmpty: Boolean = message.isEmpty

  /** Conditionally displays the message, according to SLF4J's logging precedence rules */
  def display(): Unit = level match {
    case Level.DEBUG => logger.debug(message)
    case Level.ERROR => logger.error(message)
    case Level.INFO  => logger.info(message)
    case Level.TRACE => logger.trace(message)
    case Level.WARN  => logger.warn(message)
  }

  lazy val nonEmpty: Boolean = message.nonEmpty
}

/** Backed by a cache, which is preloaded with `"git" -> which(git)`, etc */
class CommandLine(implicit config: Config = Config.default) {
  import java.io.File
  import java.nio.file.{Path, Paths}
  import java.util.regex.Pattern
  import scala.sys.process._
  import scala.util.Properties.isWin

  var lastResult = ""

  protected val cmdNameCache: mutable.Map[String, Path] =
    mutable.HashMap(
      "git"      -> whichOrThrow("git", useCache=false),
      "sbt"      -> whichOrThrow("sbt", useCache=false),
      "scaladoc" -> whichOrThrow("scaladoc", useCache=false)
    )

  /** Change current directory
    * @return Current directory */
  @inline def cd(dir: File): File = {
    System.setProperty("user.dir", dir.getPath)
    new File(sys.props("user.dir"))
  }

  /** Change current directory
    * @return Current directory */
  @inline def cd(dir: Path): Path = cd(dir.toFile).toPath

  /** Change current directory
    * @return Current directory */
  @inline def cd(dir: String): String = cd(new File(dir)).getPath

  @inline def run(cmd: String)
                 (implicit logMessage: LogMessage, log: Logger): String =
    run(new File(sys.props("user.dir")), cmd)


  @inline def run(cmd: String*)
                 (implicit logMessage: LogMessage, log: Logger): String =
    run(new File(sys.props("user.dir")), cmd: _*)


  def run(cwd: File = new File(sys.props("user.dir")), cmd: String)
         (implicit logMessage: LogMessage, log: Logger): String = {
    val tokens: Array[String] = cmd.split(" ")
    val command: List[String] = whichOrThrow(tokens(0)).toString :: tokens.tail.toList
    if (logMessage.nonEmpty) logMessage.display()
    log.debug(s"# Running $cmd from '$cwd'") //, which translates to ${ command.mkString("\"", "\", \"", "\"") }")
    lastResult = if (config.dryRun && (tokens.take(2) sameElements Array(which("git"), "commit"))) {
      log.debug("# " + command.mkString(" "))
      run(cwd, "git status")
    } else
      Process(command=command, cwd=cwd).!!.trim
    lastResult
  }


  def run(cwd: File, cmd: String*)
         (implicit logMessage: LogMessage, log: Logger): String = {
    val command: List[String] = whichOrThrow(cmd(0)).toString :: cmd.tail.toList
    if (logMessage.nonEmpty) logMessage.display()
    log.debug(s"Running ${ cmd.mkString(" ") } from '$cwd'")
    lastResult = if (config.dryRun && (cmd.take(2) == Seq(which("git"), "commit"))) {
      log.debug(s"# $cmd")
      run(cwd, "git", "status")
    } else
      Process(command=command, cwd=cwd).!!.trim
    lastResult
  }


  @inline def run(cwd: Path, cmd: String)
         (implicit logMessage: LogMessage, log: Logger): String =
    run(cwd.toFile, cmd)


  @inline def run(cwd: Path, cmd: String*)
         (implicit logMessage: LogMessage, log: Logger): String =
    run(cwd.toFile, cmd: _*)


  protected lazy val pathEnv: String = sys.env.getOrElse("PATH", sys.env.getOrElse("Path", sys.env("path")))

  protected lazy val paths: Array[Path] =
    pathEnv
      .split(Pattern.quote(File.pathSeparator))
      .map(Paths.get(_))

  def which(programName: String, useCache: Boolean = true): Option[Path] =
    (if (useCache) cmdNameCache.get(programName) else None) orElse {
      val result: Option[Path] = paths.collectFirst {
        case path if resolve(path, programName).exists(_.toFile.exists)                  => resolve(path, programName)

        case path if isWin && resolve(path, s"$programName.cmd").exists(_.toFile.exists) => resolve(path, s"$programName.cmd")

        case path if isWin && resolve(path, s"$programName.bat").exists(_.toFile.exists) => resolve(path, s"$programName.bat")
      }.flatten

      if (useCache) result.foreach(cmdNameCache.put(programName, _))
      result
    }


  @inline protected def resolve(path: Path, program: String): Option[Path] = {
    val x = path.resolve(program)
    if (x.toFile.exists) Some(x) else None
  }

  @inline protected def whichOrThrow(program: String, useCache: Boolean = true): Path =
    which(program, useCache) match {
      case None =>
        Console.err.println(s"Error: $program not found on ${ if (isWin) "Path" else "PATH" }")
        System.exit(0)
        Paths.get("Bogus, dude, just making the compiler happy.")

      case Some(programPath) => programPath
    }
}
