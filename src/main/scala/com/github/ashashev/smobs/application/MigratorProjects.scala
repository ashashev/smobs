package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.core.bitbucket.server.Requests._

/**
  * Migrates projects.
  *
  */
class MigratorProjects(source: BitbucketServer,
                       destination: BitbucketServer,
                       transformer: ProjectTransformer,
                       extProcessing: Processing) {
  private def processing(p: Project, ds: Seq[Project]): Option[(Project, Project, Seq[Project])] = {
    transformer.transform(p, ds) match {
      case (np, ProjectTransformer.Exist) =>
        extProcessing.project(p, np, destination, true).map(ps => (ps._1, ps._2, ds))
      case (np, ProjectTransformer.NotExist) =>
        extProcessing.project(p, np, destination, false).map(ps => (ps._1, ps._2, ps._2 +: ds))
    }
  }

  private def travers(ss: Seq[Project], ds: Seq[Project]): Unit = ss.headOption match {
    case Some(p) =>
      processing(p, ds) match {
        case Some((sp, dp, ds)) =>
          MigratorRepositoies(source, destination, sp, dp, extProcessing).travers()
          travers(ss.tail, ds)
        case None => travers(ss.tail, ds)
      }
    case None => ()
  }

  def travers(): Unit = {
    travers(source.getProjects().filter(transformer.filteredProjects), destination.getProjects())
    travers(source.getUsers().filter(transformer.filteredUsers), destination.getUsers())
  }

}

object MigratorProjects {
  def apply(config: Config,
            extProcessing: Processing): MigratorProjects =
    new MigratorProjects(BitbucketServer(config.source),
      BitbucketServer(config.destination),
      ProjectTransformer(config),
      extProcessing)
}
