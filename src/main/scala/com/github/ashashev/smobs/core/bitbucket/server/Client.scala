/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.core.bitbucket.server

import java.net.HttpURLConnection._

import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JBool, JValue}
import org.json4s.native.JsonMethods

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

/**
  * Implements some functions of the BitBucket Server REST API
  *
  * For more information, see description of Bitbucket Server
  * [[https://docs.atlassian.com/bitbucket-server/rest/5.7.0/bitbucket-rest.html REST API]].
  */
class Client(url: String,
             user: String,
             password: String,
             connectionTimeoutMs: Int,
             readTimeoutMs: Int) {

  import Client._

  private implicit val jsonFormats = DefaultFormats + PermissionSerializer

  private def http(url: String) = Http(url).auth(user, password).
    timeout(connectionTimeoutMs, readTimeoutMs).header("Content-Type", "application/json")

  private def get(url: String, params: (String, String)*) =
    http(url).params(params).asString

  private def post(url: String, data: String, params: (String, String)*) = {
    http(url).postData(data).params(params).asString
  }

  private def post(url: String, request: Requests.Request) = {
    http(url).postData(request.toJson()).asString
  }

  private def put(url: String, params: (String, String)*) =
    http(url).postData("").method("PUT").params(params).asString

  private def delete(url: String, params: (String, String)*) =
    http(url).postData("").method("DELETE").params(params).asString

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
    */
  def getLfsEnable(projectKey: String, repoSlug: String): Boolean = {
    val lfsurl = Urls.lsfEnabled(url, projectKey, repoSlug)
    val response = get(lfsurl)
    response.code match {
      case HTTP_OK => true
      case HTTP_NOT_FOUND => false
      case x =>
        throw new Error(
          s"""GET: $lfsurl
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
          s"""PUT: $lfsurl
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
    */
  def getPermitsGroups(): Either[Seq[Responses.Error], Seq[Responses.GroupPermission]] = {
    import Responses._
    getPageData(Urls.permitsGroups(url))(j => Try(j.extract[Page[GroupPermission]]))
  }

  /**
    * Retrieves memebers of the group
    *
    * The authenticated user must have ADMIN permission or higher to call this
    * resource.
    */
  def getMembers(group: String): Either[Seq[Responses.Error], Seq[Responses.User]] = {
    import Responses._
    getPageData(Urls.groupMembers(url), ("context", group))(j => Try(j.extract[Page[User]]))
  }

  /**
    * Check whether the specified permission is the default permission (granted
    * to all users) for a project.
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    */
  def getDefaultPermissions(projectKey: String, permission: Permission):
  Either[Seq[Responses.Error], Boolean] = {
    val u = Urls.projectDefaultPermits(url, projectKey, permission)
    val response = get(u)
    response.code match {
      case HTTP_OK => JsonMethods.parse(response.body) \ "permitted" match {
        case JBool(v) => Right(v)
        case _ =>
          throw new Error(
            s"""$u
               |throw new Error("No usable value for permitted")
               |${response}
               |""".stripMargin)
      }
      case HttpErrorCodes(errorHeader) =>
        Try(JsonMethods.parse(response.body)).map(_.extract[Responses.Errors]) match {
          case Success(es) => Left(es.errors)
          case Failure(e) =>
            throw new Error(
              s"""$u
                 |$errorHeader
                 |${response}
                 |""".stripMargin, e)
        }
      case x =>
        throw new Error(
          s"""$u
             |Unexpected Code $x.
             |${response}
             |""".stripMargin)
    }
  }

  /**
    * Grant or revoke a project permission to all users, i.e. set the default
    * permission.
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    */
  def setDefaultPermissions(projectKey: String, permission: Permission, allow: Boolean):
  Option[Seq[Responses.Error]] = {
    val u = Urls.projectDefaultPermits(url, projectKey, permission)
    val response = post(u, "", ("allow" -> s"$allow"))
    response.code match {
      case HTTP_NO_CONTENT => None
      case HttpErrorCodes(errorHeader) =>
        Try(JsonMethods.parse(response.body)).map(_.extract[Responses.Errors]) match {
          case Success(es) => Some(es.errors)
          case Failure(e) =>
            throw new Error(
              s"""$u
                 |$errorHeader
                 |${response}
                 |""".stripMargin, e)
        }
      case x =>
        throw new Error(
          s"""$u
             |Unexpected Code $x.
             |${response}
             |""".stripMargin)
    }
  }

  /**
    * Retrieves projects.
    *
    * Only projects for which the authenticated user has the PROJECT_VIEW
    * permission will be returned.
    */
  def getProjects(): Either[Seq[Responses.Error], Seq[Responses.Project]] = {
    import Responses._
    getPageData(Urls.projects(url))(j => Try(j.extract[Page[Project]]))
  }

  private def setPermits(url: String, name: String, permission: Permission):
  Option[Seq[Responses.Error]] = {
    val response = put(url, "permission" -> permission.value, "name" -> name)
    response.code match {
      case HTTP_NO_CONTENT => None
      case HTTP_BAD_REQUEST =>
        Try(JsonMethods.parse(response.body)).map(_.extract[Responses.Errors]) match {
          case Success(es) => Some(es.errors)
          case Failure(e) =>
            throw new Error(
              s"""PUT: $url
                 |400 Bad Request.
                 |${response}
                 |""".stripMargin, e)
        }
      case HttpErrorCodes(errorHeader) =>
        Some(Seq(Responses.Error(message = errorHeader)))
      case x =>
        throw new Error(
          s"""PUT: $url
             |Unexpected Code $x.
             |${response}
             |""".stripMargin)
    }
  }

  private def revokePermits(url: String, name: String): Option[Seq[Responses.Error]] = {
    val response = delete(url, "name" -> name)
    response.code match {
      case HTTP_NO_CONTENT => None
      case HTTP_BAD_REQUEST =>
        Try(JsonMethods.parse(response.body)).map(_.extract[Responses.Errors]) match {
          case Success(es) => Some(es.errors)
          case Failure(e) =>
            throw new Error(
              s"""DELETE: $url
                 |400 Bad Request.
                 |${response}
                 |""".stripMargin, e)
        }
      case HttpErrorCodes(errorHeader) =>
        Some(Seq(Responses.Error(message = errorHeader)))
      case x =>
        throw new Error(
          s"""DELETE: $url
             |Unexpected Code $x.
             |${response}
             |""".stripMargin)
    }
  }

  /**
    * Retrieve a groups that have been granted at least one permission for the
    * specified project.
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    */
  def getProjectPermitsGroups(projectKey: String):
  Either[Seq[Responses.Error], Seq[Responses.GroupPermission]] = {
    import Responses._
    getPageData(Urls.projectPermitsGroups(url, projectKey)) { j =>
      Try(j.extract[Page[GroupPermission]])
    }
  }

  /**
    * Promote or demote a group's permission level for the specified project.
    *
    * Available project permissions are:
    * <ul>
    * <li>[[Permission.ProjectAdmin]]</li>
    * <li>[[Permission.ProjectWrite]]</li>
    * <li>[[Permission.ProjectRead]]</li>
    * </ul>
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    * In addition, a user may not demote a group's permission level if their
    * own permission level would be reduced as a result.
    */
  def setProjectPermitsGroup(projectKey: String, group: String, permission: Permission):
  Option[Seq[Responses.Error]] =
    setPermits(Urls.projectPermitsGroups(url, projectKey), group, permission)

  /**
    * Retrieve a page of users that have been granted at least one permission
    * for the specified project.
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    */
  def getProjectPermitsUsers(projectKey: String):
  Either[Seq[Responses.Error], Seq[Responses.UserPermission]] = {
    import Responses._
    getPageData(Urls.projectPermitsUsers(url, projectKey)) { j =>
      Try(j.extract[Page[UserPermission]])
    }
  }

  /**
    * Promote or demote a user's permission level for the specified project.
    *
    * Available project permissions are:
    * <ul>
    * <li>[[Permission.ProjectAdmin]]</li>
    * <li>[[Permission.ProjectWrite]]</li>
    * <li>[[Permission.ProjectRead]]</li>
    * </ul>
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    * In addition, a user may not reduce their own permission level unless they
    * have a global permission that already implies that permission.
    */
  def setProjectPermitsUser(projectKey: String, user: String, permission: Permission):
  Option[Seq[Responses.Error]] =
    setPermits(Urls.projectPermitsUsers(url, projectKey), user, permission)

  /**
    * Revoke all permissions for the specified project for a user.
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    *
    * In addition, a user may not revoke their own project permissions if they
    * do not have a higher global permission.
    */
  def revokeProjectPermitsGroup(projectKey: String, group: String): Option[Seq[Responses.Error]] =
    revokePermits(Urls.projectPermitsGroups(url, projectKey), group)

  /**
    * Revoke all permissions for the specified project for a user.
    *
    * The authenticated user must have PROJECT_ADMIN permission for the
    * specified project or a higher global permission to call this resource.
    *
    * In addition, a user may not revoke their own project permissions if they
    * do not have a higher global permission.
    */
  def revokeProjectPermitsUser(projectKey: String, user: String): Option[Seq[Responses.Error]] =
    revokePermits(Urls.projectPermitsUsers(url, projectKey), user)

  /**
    * Retrieve a groups that have been granted at least one permission
    * for the specified repository.
    *
    * The authenticated user must have REPO_ADMIN permission for the specified
    * repository or a higher project or global permission to call this resource.
    */
  def getRepoPermitsGroups(projectKey: String, slug: String):
  Either[Seq[Responses.Error], Seq[Responses.GroupPermission]] = {
    import Responses._
    getPageData(Urls.repoPermitsGroups(url, projectKey, slug)) { j =>
      Try(j.extract[Page[GroupPermission]])
    }
  }

  /**
    * Promote or demote a group's permission level for the specified repository.
    *
    * Available repository permissions are:
    * <ul>
    * <li>[[Permission.RepoAdmin]]</li>
    * <li>[[Permission.RepoWrite]]</li>
    * <li>[[Permission.RepoRead]]</li>
    * </ul>
    *
    * The authenticated user must have REPO_ADMIN permission for the specified
    * repository or a higher project or global permission to call this resource.
    * In addition, a user may not demote a group's permission level if their own
    * permission level would be reduced as a result.
    */
  def setRepoPermitsGroup(projectKey: String, slug: String, group: String, permission: Permission):
  Option[Seq[Responses.Error]] =
    setPermits(Urls.repoPermitsGroups(url, projectKey, slug), group, permission)

  /**
    * Retrieve a users that have been granted at least one permission for the
    * specified repository.
    *
    * The authenticated user must have REPO_ADMIN permission for the specified
    * repository or a higher project or global permission to call this resource.
    */
  def getRepoPermitsUsers(projectKey: String, slug: String):
  Either[Seq[Responses.Error], Seq[Responses.UserPermission]] = {
    import Responses._
    getPageData(Urls.repoPermitsUsers(url, projectKey, slug)) { j =>
      Try(j.extract[Page[UserPermission]])
    }
  }

  /**
    * Promote or demote a user's permission level for the specified repository.
    *
    * Available repository permissions are:
    * <ul>
    * <li>[[Permission.RepoAdmin]]</li>
    * <li>[[Permission.RepoWrite]]</li>
    * <li>[[Permission.RepoRead]]</li>
    * </ul>
    *
    * The authenticated user must have REPO_ADMIN permission for the specified
    * repository or a higher project or global permission to call this resource.
    * In addition, a user may not reduce their own permission level unless they
    * have a project or global permission that already implies that permission.
    */
  def setRepoPermitsUser(projectKey: String, slug: String, user: String, permission: Permission):
  Option[Seq[Responses.Error]] =
    setPermits(Urls.repoPermitsUsers(url, projectKey, slug), user, permission)

  ///////////////////////////

  /**
    * Creates a new project.
    *
    * The authenticated user must have PROJECT_CREATE permission to call this
    * resource.
    *
    * @param project the properties of new project
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

    def projectPermissions(server: String, projectKey: String) =
      makeUrl(projects(server), projectKey, "permissions")

    def projectPermitsGroups(server: String, projectKey: String) =
      makeUrl(projectPermissions(server, projectKey), "groups")

    def projectPermitsUsers(server: String, projectKey: String) =
      makeUrl(projectPermissions(server, projectKey), "users")

    def projectDefaultPermits(server: String, projectKey: String, permission: Permission) =
      makeUrl(projectPermissions(server, projectKey), permission.value, "all")

    def repoPermissions(server: String, projectKey: String, slug: String) =
      makeUrl(repositories(server, projectKey), slug, "permissions")

    def repoPermitsGroups(server: String, projectKey: String, slug: String) =
      makeUrl(repoPermissions(server, projectKey, slug), "groups")

    def repoPermitsUsers(server: String, projectKey: String, slug: String) =
      makeUrl(repoPermissions(server, projectKey, slug), "users")

    /**
      * Returns url for check whether LFS is enabled for the repository.
      *
      * For more information, see answer on the question
      * [[https://community.atlassian.com/t5/Bitbucket-questions/Enable-LFS-through-Rest-API/qaq-p/100333 "Enable LFS through Rest API"]]
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