/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoHref
import com.github.ashashev.smobs.core.bitbucket.server.Requests.Project
import com.sun.xml.internal.ws.api.message.Message

import sys.process._

object DefaultProcessing extends Processing {

  private def toStr(p: Project): String = p.name + "(" + p.key + ")"
  private def toStr(r: RepoHref): String = r._1.name
  private def collored(color: String)(f : => Unit) = {
    print(s"${Console.RESET}${color}")
    f
    print(s"${Console.RESET}")
  }

  private val YELLOW = collored(Console.YELLOW)(_)
  private val RED = collored(Console.RED)(_)

  val ident = "    "

  private final class GitException(message: String) extends Throwable(message)

  def project(sp: Project,
              dp: Project,
              dstServer: BitbucketServer,
              dstExists: Boolean): Option[(Project, Project)] = {
    val isPersonal = dp.key.startsWith("~")

    if (isPersonal && !dstExists) {
      YELLOW {
        println(s"""${toStr(sp)} -> ${toStr(dp)} [The user doesn't exist, skipped]""")
      }
      None
    } else if (dstExists) {
      println(s"""${toStr(sp)} -> ${toStr(dp)} [Exists]""")
      Some((sp, dp))
    } else {
      dstServer.createProject(dp) match {
        case Some(p) =>
          println(s"""${toStr(sp)} -> ${toStr(dp)} [Created]""")
          Some((sp, p))
        case None =>
          RED {
            println(s"""${toStr(sp)} -> ${toStr(dp)} [Creating failed, skipped]""")
          }
          None
      }
    }
  }

  def repository(sr: RepoHref,
                 dr: Option[RepoHref],
                 dst: Project,
                 dstServer: BitbucketServer): Option[RepoHref] = {
    val repoDir = "processing_repo"
    deleteDirectory(repoDir)
    if (dr.isDefined) {
      YELLOW {
        println(s"""${toStr(sr)} -> ${toStr(dr.get)} [The repository exists, skipped]""")
      }
      None
    } else {
      dstServer.createRepository(dst, sr._1) match {
        case Some(dr) =>
          try {
            if (Seq("git", "clone", "--bare", sr._2, repoDir).! != 0) {
              throw new GitException("Cloning was failed")
            }
            if (Seq("git", "-C", repoDir, "remote", "set-url", "origin", dr._2).! != 0) {
              throw new GitException("Changing remote was failed")
            }
            if (Seq("git", "-C", repoDir, "push", "--all").! != 0) {
              throw new GitException("Pushing refs was failed")
            }
            if (Seq("git", "-C", repoDir, "push", "--tags").! != 0) {
              throw new GitException("Pushing tags was failed")
            }
            println(s"""${toStr(sr)} -> ${toStr(dr)} [Created]""")
          } catch {
            case ge: GitException => RED {
              println(s"""${toStr(sr)} -> ${toStr(dr)} [${ge.getMessage()}]""")
            }
          }
          deleteDirectory(repoDir)
          Some(dr)
        case None =>
          RED {
            println(s"""${toStr(sr)} -> ${toStr(dr.get)} [Creating failed, skipped]""")
          }
          None
      }
    }
  }

}
