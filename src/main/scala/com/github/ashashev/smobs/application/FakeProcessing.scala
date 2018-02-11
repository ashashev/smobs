/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoInfo
import com.github.ashashev.smobs.core.bitbucket.server.Requests.Project

/**
  * Logs out all supposed actions.
  */
object FakeProcessing extends Processing {
  private var totalProjects = 0
  private var totalUsers = 0
  private var totalRepositories = 0
  private var generalRepositories = 0
  private var personalRepositories = 0

  def project(sp: Project,
              dp: Project,
              srcServer: BitbucketServer,
              dstServer: BitbucketServer,
              dstExists: Boolean): Option[(Project, Project)] = {
    val isPersonal = dp.key.startsWith("~")
    val state =
      if (isPersonal) if (!dstExists) "doesn't exists, will be skiped" else "exists"
      else if (!dstExists) "will be created" else "exists"

    def pTs(p: Project) = s"${p.name} (${p.key})"

    print(s"""${pTs(sp)} -> ${pTs(dp)} [${state}]""")
    if (sp.key == dp.key) println("")
    else RED { println(" Warning! Key was changed") }

    if (isPersonal)
      totalUsers += 1
    else
      totalProjects += 1

    if (isPersonal && !dstExists) None
    else Some((sp, dp))
  }

  def repository(sr: RepoInfo,
                 dr: Option[RepoInfo],
                 sp: Project,
                 dp: Project,
                 srcServer: BitbucketServer,
                 dstServer: BitbucketServer): Option[RepoInfo] = {
    val dstExists = dr.isDefined
    val lfs = if (sr.enabledLfs) " (lfs)" else ""

    def rTs(r: RepoInfo) = s"${r.repo.name}"

    val drv = dr.getOrElse(sr)

    if (dstExists) RED {
      println(s"""    ${rTs(sr)} -> ${rTs(drv)}${lfs} [exists, will be skipped]""")
    }
    else println(s"""    ${rTs(sr)} -> ${rTs(drv)}${lfs} [will be created]""")

    totalRepositories += 1

    if (dp.key.startsWith("~"))
      personalRepositories += 1
    else
      generalRepositories += 1

    if (dstExists) None
    else Some(sr)
  }

  def printStatistics(): Unit = {
    println(s"Total Projects:     $totalProjects")
    println(s"Total Users:        $totalUsers")
    println(s"Total Repositories: $totalRepositories")
    println(s"    General:        $generalRepositories")
    println(s"    Personal:       $personalRepositories")
  }

  def clearStatistics(): Unit = {
    totalProjects = 0
    totalUsers = 0
    totalRepositories = 0
    generalRepositories = 0
    personalRepositories = 0
  }
}
