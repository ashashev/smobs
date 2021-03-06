/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoInfo
import com.github.ashashev.smobs.core.bitbucket.server.Requests.Project

import sys.process._

object DefaultProcessing {

  private def toStr(p: Project): String = p.name + "(" + p.key + ")"

  private def toStr(r: RepoInfo): String = r.repo.name

  private val ident = "    "

  def apply(git: Git, copyPermissions: Boolean): DefaultProcessing =
    new DefaultProcessing(git, copyPermissions)
}

class DefaultProcessing(git: Git, copyPermissions: Boolean) extends Processing {
  import DefaultProcessing._

  private def copyProjectPermissions(sp: Project,
                                     dp: Project,
                                     srcServer: BitbucketServer,
                                     dstServer: BitbucketServer): Unit = if (copyPermissions) {
    for((group, permission) <- srcServer.getProjectPermitsGroups(sp)) {
      dstServer.setProjectPermitsGroup(dp, group, permission)
    }

    for((user, permission) <- srcServer.getProjectPermitsUsers(sp)) {
      dstServer.setProjectPermitsUser(dp, user, permission)
    }

    dstServer.setDefaultPermits(dp, srcServer.getDefaultPermits(sp))
  } else {}

  private def copyRepoPermissions(sr: RepoInfo,
                                  dr: RepoInfo,
                                  sp: Project,
                                  dp: Project,
                                  srcServer: BitbucketServer,
                                  dstServer: BitbucketServer): Unit = if (copyPermissions) {
    for((group, permission) <- srcServer.getRepoPermitsGroups(sp, sr)) {
      dstServer.setRepoPermitsGroup(dp, dr, group, permission)
    }

    for((user, permission) <- srcServer.getRepoPermitsUsers(sp, sr)) {
      dstServer.setRepoPermitsUser(dp, dr, user, permission)
    }
  } else {}

  def project(sp: Project,
              dp: Project,
              srcServer: BitbucketServer,
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
          copyProjectPermissions(sp, p, srcServer, dstServer)
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
                 sp: Project,
                 dp: Project,
                 srcServer: BitbucketServer,
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
      dstServer.createRepository(dp, sr.repo, sr.enabledLfs) match {
        case Some(dr) =>
          try {
            copyRepoPermissions(sr, dr, sp, dp, srcServer, dstServer)

            assert(sr.enabledLfs == dr.enabledLfs)

            val repo = git.clone(sr.href, repoDir)

            if (sr.enabledLfs) {
              repo.fetchLfs()
            }

            repo.setOrigin(dr.href)

            repo.pushAll()

            if (sr.enabledLfs) {
              repo.pushLfsAll()
            }

            repo.close()

            println(s"""${toStr(sr)} -> ${toStr(dr)} [Created]""")
          } catch {
            case ge: Git.GitException => RED {
              println(s"""${toStr(sr)} -> ${toStr(dr)} [${ge.getMessage()}]""")
            }
          }

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
