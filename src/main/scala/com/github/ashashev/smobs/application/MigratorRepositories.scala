package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.BitbucketServer.RepoHref
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
  private def processing(r: RepoHref, dsr: Seq[RepoHref]): Seq[RepoHref] = {
    extProcessing.repository(r, dsr.find(_._1.name == r._1.name), dp, destination) match {
      case Some(r) => r +: dsr
      case None => dsr
    }
  }

  private def travers(ssr: Seq[RepoHref], dsr: Seq[RepoHref]): Unit = ssr.headOption match {
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