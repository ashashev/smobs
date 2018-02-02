/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoInfo
import com.github.ashashev.smobs.core.bitbucket.server.Requests.Project
import com.sun.xml.internal.ws.api.message.Message

import sys.process._

object DefaultProcessing extends Processing {

  private def toStr(p: Project): String = p.name + "(" + p.key + ")"
  private def toStr(r: RepoInfo): String = r.repo.name

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
      print(s"""${toStr(sp)} -> ${toStr(dp)} [Exists]""")
      if (sp.key == dp.key) println("")
      else RED { println("Warning! Key was changed") }
      Some((sp, dp))
    } else {
      dstServer.createProject(dp) match {
        case Some(p) =>
          print(s"""${toStr(sp)} -> ${toStr(dp)} [Created]""")
          if (sp.key == dp.key) println("")
          else RED { println(" Warning! Key was changed") }
          Some((sp, p))
        case None =>
          RED {
            println(s"""${toStr(sp)} -> ${toStr(dp)} [Creating failed, skipped]""")
          }
          None
      }
    }
  }

  def repository(sr: RepoInfo,
                 dr: Option[RepoInfo],
                 dst: Project,
                 dstServer: BitbucketServer): Option[RepoInfo] = {
    val repoDir = "processing_repo"
    deleteDirectory(repoDir)
    println(s"""Start processing the "${toStr(sr)}" repository...""")
    if (dr.isDefined) {
      YELLOW {
        println(s"""${toStr(sr)} -> ${toStr(dr.get)} [The repository exists, skipped]""")
      }
      None
    } else {
      dstServer.createRepository(dst, sr.repo, sr.enabledLfs) match {
        case Some(dr) =>
          try {
            assert(sr.enabledLfs == dr.enabledLfs)
            if (Seq("git", "clone", "--bare", sr.href, repoDir).! != 0) {
              throw new GitException("Cloning was failed")
            }
            if (sr.enabledLfs) {
              if (Seq("git", "-C", repoDir, "lfs", "fetch", "--all").! != 0) {
                throw new GitException("Fetching lfs was failed")
              }
            }
            if (Seq("git", "-C", repoDir, "remote", "set-url", "origin", dr.href).! != 0) {
              throw new GitException("Changing remote was failed")
            }
            if (Seq("git", "-C", repoDir, "push", "--all").! != 0) {
              throw new GitException("Pushing refs was failed")
            }
            if (Seq("git", "-C", repoDir, "push", "--tags").! != 0) {
              throw new GitException("Pushing tags was failed")
            }
            if (sr.enabledLfs) {
              if (Seq("git", "-C", repoDir, "lfs", "push", "origin", "--all").! != 0) {
                throw new GitException("Pushing lfs was failed")
              }
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
