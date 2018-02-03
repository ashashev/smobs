/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.core.bitbucket.server

import java.net.HttpURLConnection._

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
class Client(url: String,
             user: String,
             password: String,
             connectionTimeoutMs: Int,
             readTimeoutMs: Int) {

  import Client._

  private implicit val jsonFormats = DefaultFormats

  private def http(url: String) = Http(url).auth(user, password).
    timeout(connectionTimeoutMs, readTimeoutMs)

  private def get(url: String, params: (String, String)*) =
    http(url).params(params).asString

  private def post(url: String, request: Requests.Request) = {
    http(url).postData(request.toJson()).
      header("Content-Type", "application/json").asString
  }

  private def put(url: String) =
    http(url).postData("").method("PUT").asString

  private def delete(url: String) =
    http(url).method("DELETE").asString

  private def getPageData[Data](url: String, params: (String, String)*)
                               (extractor: JValue => Try[Responses.Page[Data]])
  : Either[Seq[Responses.Error], Seq[Data]] = {
    def accumulate(start: Int, acc: ListBuffer[Data] = ListBuffer.empty)
    : Either[Seq[Responses.Error], Seq[Data]] = {
      val response = get(url, (("start", start.toString) +: params): _*)
      response.code match {
        case HTTP_OK => Try(JsonMethods.parse(response.body)).flatMap(extractor) match {
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
      case HTTP_CREATED => Try(JsonMethods.parse(response.body)).flatMap(extractor) match {
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
    * Returns if LFS is switched on for the repository.
    *
    * @param projectKey
    * @param repoSlug
    * @return
    */
  def getLfsEnable(projectKey: String, repoSlug: String): Boolean = {
    val lfsurl = Urls.lsfEnabled(url, projectKey, repoSlug)
    val response = get(lfsurl)
    response.code match {
      case HTTP_OK => true
      case HTTP_NOT_FOUND => false
      case x =>
        throw new Error(
          s"""$lfsurl
             |Unexpected Code $x.
             |${response}
             |""".stripMargin)
    }
  }

  def setLfsEnable(projectKey: String, repoSlug: String, enabled: Boolean) = {
    val lfsurl = Urls.lsfEnabled(url, projectKey, repoSlug)
    val response = if (enabled) put(lfsurl) else delete(lfsurl)
    response.code match {
      case HTTP_NO_CONTENT => ()
      case x =>
        throw new Error(
          s"""$lfsurl
             |Unexpected Code $x.
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
    * Retrieves groups that have been granted at least one global permission.
    *
    * The authenticated user must have ADMIN permission or higher to call this
    * resource.
    *
    * @return
    */
  def getPermitsGroups(): Either[Seq[Responses.Error], Seq[Responses.GroupPermission]] = {
    import Responses._
    getPageData(Urls.permitsGroups(url))(j => Try(j.extract[Page[GroupPermission]]))
  }

  def getMembers(group: String): Either[Seq[Responses.Error], Seq[Responses.User]] = {
    import Responses._
    getPageData(Urls.groupMembers(url), ("context", group))(j => Try(j.extract[Page[User]]))
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
    private val lsfApi = "rest/git-lfs/admin"

    def projects(server: String) = makeUrl(server, api, "projects")

    def repositories(server: String, projectKey: String) =
      makeUrl(projects(server), projectKey, "repos")

    def admin(server: String) = makeUrl(server, api, "admin")

    def permisions(server: String) = makeUrl(admin(server), "/permissions")

    def permitsUsers(server: String) = makeUrl(permisions(server), "users")

    def permitsGroups(server: String) = makeUrl(permisions(server), "groups")

    def groups(server: String) = makeUrl(admin(server), "groups")

    def groupMembers(server: String) = makeUrl(groups(server), "more-members")

    /**
      * Returns url for check whether LFS is enabled for the repository.
      *
      * For more information, see answer on the question
      * [[https://community.atlassian.com/t5/Bitbucket-questions/Enable-LFS-through-Rest-API/qaq-p/100333 "Enable LFS through Rest API"]]
      *
      * @param server
      * @param projectKey
      * @param repoSlug
      * @return
      */
    def lsfEnabled(server: String,
                   projectKey: String,
                   repoSlug: String): String = {
      makeUrl(server, lsfApi, "projects", projectKey, "repos", repoSlug, "enabled")
    }
  }

  def apply(url: String,
            user: String,
            password: String,
            connectionTimeoutMs: Int = 1000,
            readTimeoutMs: Int = 5000): Client =
    new Client(url, user, password,
      connectionTimeoutMs: Int, readTimeoutMs: Int)
}