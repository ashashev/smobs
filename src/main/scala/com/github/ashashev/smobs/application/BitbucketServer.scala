package com.github.ashashev.smobs.application

import com.github.ashashev.smobs._
import com.github.ashashev.smobs.core.bitbucket.server._

/**
  * It's wrapper over the [[core.bitbucket.server.Client]]
  */
class BitbucketServer(url: String, user: String, password: String) {
  import BitbucketServer._
  private val client = core.bitbucket.server.Client(url, user, password)

  private def output(error: Responses.Error, indention: Boolean): Unit = {
    val indent = if (indention) "    " else ""

    if (error.context.isDefined) {
      Console.err.println(s"${indent}context: ${error.context.get}")
    }

    Console.err.println(s"${indent}message: ${error.message}")

    if (error.exceptionName.isDefined) {
      Console.err.println(s"${indent}exception: ${error.exceptionName.get}")
    }
  }

  private def getSshHref(r: Responses.Repository): String = {
    r.links.clone.find(_.name == "ssh") match {
      case Some(l) => l.href
      case None => throw new Error(
        s"""Can't get ssh url for clone:
           |    server: $url
           |    project: ${r.project.key}
           |    project name: ${r.project.name}
           |    repository: ${r.name}
           |    repository: ${r.links}
           |""".stripMargin)
    }
  }

  def getProjects(): Seq[Requests.Project] = {
    client.getProjects().map(_.map(Requests(_))) match {
      case Right(ps) => ps
      case Left(es) =>
        Console.err.println(
          s"""The operation of requesting projects was failed.
             |    server: $url
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        Seq.empty
    }
  }

  def getUsers(): Seq[Requests.Project] = {
    client.getPermitsUsers().map(_.map { up =>
      Requests.Project(s"~${up.user.slug}", up.user.name, None)
    }) match {
      case Right(us) => us
      case Left(es) =>
        Console.err.println(
          s"""The operation of requesting users was failed.
             |    server: $url
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        Seq.empty
    }
  }

  /**
    * Returns existing repositories exclude forks
    *
    * @param project
    * @return
    */
  def getRepositories(project: Requests.Project): Seq[RepoHref] = {
    client.getRepositories(project.key).
      map(
        _.withFilter(_.origin.isEmpty).
          map { r =>
            (Requests(r), getSshHref(r))
          }) match {
      case Right(us) => us
      case Left(es) =>
        Console.err.println(
          s"""The operation of requesting repositories was failed.
             |    server: $url
             |    project: ${project.key}
             |    project name: ${project.name}
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        Seq.empty
    }
  }

  def createProject(project: Requests.Project): Option[Requests.Project] = {
    client.createProject(project).map(Requests(_)) match {
      case Right(p) => Some(p)
      case Left(es) =>
        Console.err.println(
          s"""The operation of creating project was failed.
             |    server: $url
             |    project: $project
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        None
    }
  }

  def createRepository(project: Requests.Project,
                       repository: Requests.Repository): Option[RepoHref] = {
    client.createRepositories(project.key, repository) match {
      case Right(r) => Some(Requests(r), getSshHref(r))
      case Left(es) =>
        Console.err.println(
          s"""The operation of creatinging repository was failed.
             |    server: $url
             |    project: $project
             |    repository: $repository
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        None
    }
  }
}

object BitbucketServer {
  type RepoHref = (Requests.Repository, String)

  def apply(url: String, user: String, password: String): BitbucketServer =
    new BitbucketServer(url, user, password)

  def apply(server: Server): BitbucketServer =
    new BitbucketServer(server.url, server.user, server.password)
}
