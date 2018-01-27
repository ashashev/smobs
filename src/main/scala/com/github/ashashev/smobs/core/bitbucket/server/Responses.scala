package com.github.ashashev.smobs.core.bitbucket.server

/**
  * A responses from Bitbucket Server can be represented by these case classes.
  *
  * For more information, see description of Bitbucket Server
  * [[https://docs.atlassian.com/bitbucket-server/rest/5.7.0/bitbucket-rest.html REST API]].
  */
object Responses {

  case class Link(href: String, name: String = "")

  case class Links(override val clone: Seq[Link], self: Seq[Link])

  case class Project(key: String,
                     id: Int,
                     name: String,
                     description: Option[String],
                     public: Option[Boolean],
                     `type`: String,
                     links: Links)

  case class Repository(slug: String,
                        id: Int,
                        name: String,
                        scmId: String,
                        state: String,
                        statusMessage: String,
                        forkable: Boolean,
                        origin: Option[Repository],
                        project: Project,
                        public: Boolean,
                        links: Links)

  case class User(name: String,
                  emailAddress: String,
                  id: Int,
                  displayName: String,
                  active: Boolean,
                  slug: String,
                  `type`: String,
                  links: Links)

  case class UserPermission(user: User, permission: String)

  case class Page[T](size: Int,
                     limit: Int,
                     isLastPage: Boolean,
                     values: Seq[T],
                     start: Int,
                     filter: Option[String],
                     nextPageStart: Int = 0)

  case class Error(context: Option[String] = None,
                   message: String = "",
                   exceptionName: Option[String] = None)

  case class Errors(errors: Seq[Error])

}
