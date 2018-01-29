/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.core.bitbucket.server

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue
import org.json4s.native.JsonMethods

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

/**
  * Implements some functions of the BitBucket Server REST API
  *
  * For more information, see description of Bitbucket Server
  * [[https://docs.atlassian.com/bitbucket-server/rest/5.7.0/bitbucket-rest.html REST API]].
  *
  * @param url
  * @param user
  * @param password
  */
class Client(url: String, user: String, password: String) {

  import Client._

  private implicit val jsonFormats = DefaultFormats

  private def http(url: String) = Http(url).auth(user, password)

  private def get(url: String, params: (String, String)*) =
    http(url).params(params).asString

  private def post(url: String, request: Requests.Request) = {
    http(url).postData(request.toJson()).
      header("Content-Type", "application/json").asString
  }

  private def getPageData[Data](url: String)
                               (extractor: JValue => Try[Responses.Page[Data]])
  : Either[Seq[Responses.Error], Seq[Data]] = {
    def accumulate(start: Int, acc: ListBuffer[Data] = ListBuffer.empty)
    : Either[Seq[Responses.Error], Seq[Data]] = {
      val response = get(url, ("start", start.toString))
      response.code match {
        case 200 => Try(JsonMethods.parse(response.body)).flatMap(extractor) match {
          case Success(page) =>
            if (page.isLastPage) Right((acc ++ page.values).result)
            else accumulate(page.nextPageStart, acc ++ page.values)
          case Failure(e) =>
            throw new Error(
              s"""$url
                 |HTTP 200 OK.
                 |${response}
                 |""".stripMargin, e)
        }
        case HttpErrorCodes(errorHeader) =>
          Try(JsonMethods.parse(response.body)).map(_.extract[Responses.Errors]) match {
            case Success(es) => Left(es.errors)
            case Failure(e) =>
              throw new Error(
                s"""$url
                   |$errorHeader.
                   |${response}
                   |""".stripMargin, e)
          }
        case x =>
          throw new Error(
            s"""$url
               |Unexpected Code $x.
               |${response}
               |""".stripMargin)
      }
    }

    accumulate(0)
  }

  private def post[T](url: String,
                      request: Requests.Request,
                      extractor: JValue => Try[T])
  : Either[Seq[Responses.Error], T] = {
    val response = post(url, request)
    response.code match {
      case 201 => Try(JsonMethods.parse(response.body)).flatMap(extractor) match {
        case Success(result) => Right(result)
        case Failure(e) =>
          throw new Error(
            s"""$url
               |HTTP 201 OK.
               |Request: ${request.toJson()}
               |${response}
               |
             """.stripMargin, e)
      }
      case HttpErrorCodes(errorHeader) =>
        Try(JsonMethods.parse(response.body)).map(_.extract[Responses.Errors]) match {
          case Success(es) => Left(es.errors)
          case Failure(e) =>
            throw new Error(
              s"""$url
                 |$errorHeader.
                 |Request: ${request.toJson()}
                 |${response}
                 |""".stripMargin, e)
        }
      case x =>
        throw new Error(
          s"""$url
             |Unexpected Code $x.
             |Request: ${request.toJson()}
             |${response}
             |""".stripMargin)
    }
  }

  /**
    * Retrieves users that have been granted at least one global permission.
    *
    * The authenticated user must have ADMIN permission or higher to call this
    * resource.
    *
    * @return
    */
  def getPermitsUsers(): Either[Seq[Responses.Error], Seq[Responses.UserPermission]] = {
    import Responses._
    getPageData(Urls.permitsUsers(url))(j => Try(j.extract[Page[UserPermission]]))
  }

  /**
    * Retrieves projects.
    *
    * Only projects for which the authenticated user has the PROJECT_VIEW
    * permission will be returned.
    *
    * @return
    */
  def getProjects(): Either[Seq[Responses.Error], Seq[Responses.Project]] = {
    import Responses._
    getPageData(Urls.projects(url))(j => Try(j.extract[Page[Project]]))
  }

  /**
    * Creates a new project.
    *
    * The authenticated user must have PROJECT_CREATE permission to call this
    * resource.
    *
    * @param project the properties of new project
    * @return
    */
  def createProject(project: Requests.Project): Either[Seq[Responses.Error], Responses.Project] = {
    import Responses._
    post(Urls.projects(url), project, (j => Try(j.extract[Project])))
  }

  /**
    * Retrieves repositories from the project corresponding to the supplied
    * projectKey.
    *
    * This method can also be invoked with the user's slug prefixed by tilde (~)
    * as the project key. E.g. "~johnsmith".
    *
    * The authenticated user must have REPO_READ permission for the specified project to call this resource.
    *
    * @param projectKey the project key
    * @return
    */
  def getRepositories(projectKey: String): Either[Seq[Responses.Error], Seq[Responses.Repository]] = {
    import Responses._
    getPageData(Urls.repositories(url, projectKey))(j => Try(j.extract[Page[Repository]]))
  }

  /**
    * Creates a new repository.
    *
    * Requires an existing project in which this repository will be created.
    * The only parameters which will be used are name and scmId.
    *
    * This method can also be invoked with the user's slug prefixed by tilde (~)
    * as the project key. E.g. "~johnsmith".
    *
    * The authenticated user must have PROJECT_ADMIN permission for the context
    * project to call this resource.
    *
    * @param projectKey the project key
    * @param repository the parameters of new repository
    * @return
    */
  def createRepositories(projectKey: String, repository: Requests.Repository): Either[Seq[Responses.Error], Responses.Repository] = {
    import Responses._
    post(Urls.repositories(url, projectKey), repository, (j => Try(j.extract[Repository])))
  }
}

object Client {

  import com.github.ashashev.smobs.core._

  private object Urls {
    private val api = "rest/api/1.0"

    def projects(server: String) = makeUrl(server, api, "projects")

    def repositories(server: String, projectKey: String) =
      makeUrl(projects(server), projectKey, "repos")

    def permisions(server: String) = makeUrl(server, api, "admin/permissions")

    def permitsUsers(server: String) = makeUrl(permisions(server), "users")
  }

  def apply(url: String, user: String, password: String): Client =
    new Client(url, user, password)
}