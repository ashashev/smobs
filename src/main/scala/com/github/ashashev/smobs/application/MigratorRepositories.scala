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
  * Migrates repositories. The source and destination projects have to exist.
  *
  * @param source
  * @param destination
  * @param sp
  * @param dp
  */
class MigratorRepositoies(source: BitbucketServer,
                          destination: BitbucketServer,
                          sp: Project,
                          dp: Project,
                          extProcessing: Processing) {
  private def processing(r: RepoInfo, dsr: Seq[RepoInfo]): Seq[RepoInfo] = {
    extProcessing.repository(r, dsr.find(_.repo.name == r.repo.name),
      sp, dp, source, destination) match {
      case Some(r) => r +: dsr
      case None => dsr
    }
  }

  private def travers(ssr: Seq[RepoInfo], dsr: Seq[RepoInfo]): Unit = ssr.headOption match {
    case Some(r) => travers(ssr.tail, processing(r, dsr))
    case None => ()
  }

  def travers(): Unit =
    travers(source.getRepositories(sp), destination.getRepositories(dp))
}

object MigratorRepositoies {
  def apply(source: BitbucketServer,
            destination: BitbucketServer,
            sp: Project,
            dp: Project,
            processing: Processing): MigratorRepositoies =
    new MigratorRepositoies(source, destination, sp, dp, processing)
}