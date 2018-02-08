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
  * Makes real work.
  */
trait Processing {
  /**
    * Processes project
    *
    * @param sp project on the source server
    * @param dp project on the the destination server
    * @param srcServer the source server
    * @param dstServer the destination server
    * @param dstExists if project exists on the destination server
    * @return
    */
  def project(sp: Project,
              dp: Project,
              srcServer: BitbucketServer,
              dstServer: BitbucketServer,
              dstExists: Boolean): Option[(Project, Project)]

  /**
    * Processes repository
    *
    * @param sr repository on the source server
    * @param dr repository on the destination server. it it doesn't exists dr is None
    * @param sp project on the source server
    * @param dp project on the the destination server
    * @param srcServer the source server
    * @param dstServer the destination server
    * @return destination repository if it creates otherwise None
    */
  def repository(sr: RepoInfo,
                 dr: Option[RepoInfo],
                 sp: Project,
                 dp: Project,
                 srcServer: BitbucketServer,
                 dstServer: BitbucketServer): Option[RepoInfo]
}
