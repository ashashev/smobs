/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs._
import com.github.ashashev.smobs.core.bitbucket.server._

/**
  * It's wrapper over the [[core.bitbucket.server.Client]]
  */
class BitbucketServer(url: String,
                      user: String,
                      password: String,
                      connectionTimeoutMs: Int,
                      readTimeoutMs: Int) {

  import BitbucketServer._

  private val client = core.bitbucket.server.Client(url, user, password,
    connectionTimeoutMs, readTimeoutMs)

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

  private def getHttpHref(r: Responses.Repository): String = {
    r.links.clone.find(_.name.startsWith("http")) match {
      case Some(l) => l.href
      case None => throw new Error(
        s"""Can't get a http(s) url for clone:
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

  /**
    * Retrieves all users that have been granted at least one global permission,
    * include members of groups that have been granted at least one global
    * permission.
    *
    * @return
    */
  def getUsers(): Seq[Requests.Project] = {
    val users = client.getPermitsUsers().map(_.map { up =>
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

    val groups = client.getPermitsGroups().map(_.map {
      gp => gp.group.name
    }) match {
      case Right(gs) => gs
      case Left(es) =>
        Console.err.println(
          s"""The operation of requesting groups was failed.
             |    server: $url
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        Seq.empty
    }

    val members = groups flatMap { group =>
      client.getMembers(group).map(_.map { u =>
        Requests.Project(s"~${u.slug}", u.name, None)
      }) match {
        case Right(us) => us
        case Left(es) =>
          Console.err.println(
            s"""The operation of requesting members  was failed.
               |    server: $url
               |    group: $group
               |Errors:""".stripMargin)
          es.foreach(output(_, true))
          Seq.empty
      }
    }

    (users.toSet ++ members).toSeq
  }

  /**
    * Returns existing repositories exclude forks
    *
    * @param project
    * @return
    */
  def getRepositories(project: Requests.Project): Seq[RepoInfo] = {
    client.getRepositories(project.key).
      map(
        _.withFilter(_.origin.isEmpty).
          map { r =>
            RepoInfo(
              Requests(r),
              getHttpHref(r),
              client.getLfsEnable(project.key, r.slug)
            )
          }) match {
      case Right(us) => us
      case Left(es) =>
        Console.err.println(
          s"""The operation of requesting repositories was failed.
             |    server: $url
             |    project key: ${project.key}
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
             |    project key: ${project.key}
             |    project name: ${project.name}
             |    project description: ${project.description.getOrElse("")}
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        None
    }
  }

  def createRepository(project: Requests.Project,
                       repository: Requests.Repository,
                       enabledLfs: Boolean): Option[RepoInfo] = {
    client.createRepositories(project.key, repository) match {
      case Right(r) =>
        client.setLfsEnable(project.key, r.slug, enabledLfs)
        val lfs = client.getLfsEnable(project.key, r.slug)
        assert(enabledLfs == lfs)
        Some(RepoInfo(Requests(r), getHttpHref(r), lfs))
      case Left(es) =>
        Console.err.println(
          s"""The operation of creatinging repository was failed.
             |    server: $url
             |    project key: ${project.key}
             |    project name: ${project.name}
             |    repository name: ${repository.name}
             |    repository scmId: ${repository.scmId}
             |    repository forkable: ${repository.forkable}
             |Errors:""".stripMargin)
        es.foreach(output(_, true))
        None
    }
  }
}

object BitbucketServer {

  final case class RepoInfo(repo: Requests.Repository,
                            href: String,
                            enabledLfs: Boolean)

  def apply(url: String,
            user: String,
            password: String,
            connectionTimeoutMs: Int,
            readTimeoutMs: Int): BitbucketServer =
    new BitbucketServer(url, user, password,
      connectionTimeoutMs: Int, readTimeoutMs: Int)

  def apply(server: Server): BitbucketServer =
    new BitbucketServer(server.url, server.user, server.password,
      server.connectionTimeoutMs, server.readTimeoutMs)
}
