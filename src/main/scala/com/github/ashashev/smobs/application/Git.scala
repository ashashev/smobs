/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import java.net.URL

import scala.sys.process._

class Git(credential: Option[Git.Credential]) {

  import Git._

  def clone(remote: String, workdir: String): BareRepo = new BareRepo(remote, workdir)

  class BareRepo(remote: String, workdir: String) {
    deleteDirectory(workdir)
    credential.map(_.init())

    private val baseClone: Seq[String] = credential match {
      case Some(c) => Seq("git", "-c", c())
      case None => Seq("git")
    }
    implicit private val baseCmds: Seq[String] = baseClone ++ Seq("-C", workdir)

    private def cmd(command: String, params: String*)(implicit base: Seq[String]): Cmd = {
      new Cmd(base ++: command +: params)
    }

    private class Cmd(parts: Seq[String]) {
      def ifFail(f: String => Unit): Unit = {
        if (parts.! != 0) {
          val cmd: String = parts.map(p =>
            if (p.trim().contains(" ")) s""""$p""""
            else p
          ).mkString(" ")
          f(cmd)
        }
      }
    }

    cmd("clone", "--bare", remote, workdir)(baseClone).ifFail { cmd =>
      throw new GitException(cmd, "Cloning was failed")
    }

    def fetchLfs() = {
      cmd("lfs", "fetch", "--all").ifFail { cmd =>
        throw new GitException(cmd, "Fetching lfs was failed")
      }
    }

    def setOrigin(remote: String) = {
      cmd("remote", "set-url", "origin", remote).ifFail { cmd =>
        throw new GitException(cmd, "Changing remote was failed")
      }
    }

    def pushAll() = {
      cmd("push", "--all").ifFail { cmd =>
        throw new GitException(cmd, "Pushing refs was failed")
      }

      cmd("push", "--tags").ifFail { cmd =>
        throw new GitException(cmd, "Pushing tags was failed")
      }
    }

    def pushLfsAll() = {
      cmd("lfs", "push", "origin", "--all").ifFail { cmd =>
        throw new GitException(cmd, "Pushing lfs was failed")
      }
    }

    def close() = {
      deleteDirectory(workdir)
      credential.map(_.deinit())
    }
  }

}

object Git {

  final class GitException(val cmd: String, val message: String) extends Throwable(message)

  class Credential(fileName: String, authData: Set[String]) {

    import java.io._

    private val file = new File(fileName)

    def init() = {
      val pw = new PrintWriter(file)
      authData.foreach(pw.write(_))
      pw.close()
    }

    def deinit() = {
      if (file.exists())
        file.delete()
    }

    def apply(): String = {
      s"credential.helper=store --file ${file.getAbsolutePath()}"
    }
  }

  object Credential {
    def apply(fileName: String, servers: Set[Server]): Credential =
      new Credential(fileName, servers.map { server =>
        val url = new URL(server.url)
        s"""${url.getProtocol()}://${server.user}:${server.password}@${url.getAuthority()}"""
      })
  }

  def apply(credential: Option[Credential]): Git =
    new Git(credential)
}
