package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoHref
import com.github.ashashev.smobs.core.bitbucket.server.Requests.Project

/**
  * Makes real work.
  */
trait Processing {
  /**
    * Processes project
    *
    * @param sp
    * @param dp
    * @param dstServer
    * @param dstExists
    * @return
    */
  def project(sp: Project,
              dp: Project,
              dstServer: BitbucketServer,
              dstExists: Boolean): Option[(Project, Project)]

  /**
    * Processes repository
    *
    * @param sr
    * @param dr
    * @param dst
    * @param dstServer
    * @return destination repository if it creates otherwise None
    */
  def repository(sr: RepoHref,
                 dr: Option[RepoHref],
                 dst: Project,
                 dstServer: BitbucketServer): Option[RepoHref]
}
