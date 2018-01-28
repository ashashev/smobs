package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoHref
import com.github.ashashev.smobs.core.bitbucket.server.Requests.Project

/**
  * Logs out all supposed actions.
  */
object FakeProcessing extends Processing {
  def project(sp: Project,
              dp: Project,
              dstServer: BitbucketServer,
              dstExists: Boolean): Option[(Project, Project)] = {
    def bTs(value: Boolean) = if (!value) "will be created" else "exists"

    def pTs(p: Project) = s"${p.name} (${p.key})"

    println(s"""${pTs(sp)} -> ${pTs(dp)} [${bTs(dstExists)}]""")
    Some((sp, dp))
  }

  def repository(sr: RepoHref,
                 dr: Option[RepoHref],
                 dst: Project,
                 dstServer: BitbucketServer): Option[RepoHref] = {
    def bTs(value: Boolean) = if (!value) "will be created" else "exists, will be skip"

    def rTs(r: RepoHref) = s"${r._1.name}"// (${r._2})"

    val dstExists = dr.isDefined

    val drv = dr.getOrElse(sr)

    println(s"""    ${rTs(sr)} -> ${rTs(drv)} [${bTs(dstExists)}]""")

    if (dstExists) None
    else Some(sr)
  }

}
