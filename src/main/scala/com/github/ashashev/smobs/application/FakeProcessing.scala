package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoHref
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
              dstServer: BitbucketServer,
              dstExists: Boolean): Option[(Project, Project)] = {
    val isPersonal = dp.key.startsWith("~")
    val state =
      if (isPersonal) if (!dstExists) "doesn't exists, will be skiped" else "exists"
      else if (!dstExists) "will be created" else "exists"

    def pTs(p: Project) = s"${p.name} (${p.key})"

    println(s"""${pTs(sp)} -> ${pTs(dp)} [${state}]""")

    if (isPersonal)
      totalUsers += 1
    else
      totalProjects += 1

    if (isPersonal && !dstExists) None
    else Some((sp, dp))
  }

  def repository(sr: RepoHref,
                 dr: Option[RepoHref],
                 dst: Project,
                 dstServer: BitbucketServer): Option[RepoHref] = {
    def bTs(value: Boolean) = if (!value) "will be created" else "exists, will be skiped"

    def rTs(r: RepoHref) = s"${r._1.name}"// (${r._2})"

    val dstExists = dr.isDefined

    val drv = dr.getOrElse(sr)

    println(s"""    ${rTs(sr)} -> ${rTs(drv)} [${bTs(dstExists)}]""")

    totalRepositories += 1

    if (dst.key.startsWith("~"))
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
