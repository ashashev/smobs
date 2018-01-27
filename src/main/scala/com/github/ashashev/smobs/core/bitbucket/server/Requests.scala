package com.github.ashashev.smobs.core.bitbucket.server

import org.json4s.DefaultFormats
import org.json4s.native.Serialization

/**
  * These case classes simplify a creating post requests to Bitbucket Server.
  *
  * For more information, see description of Bitbucket Server
  * [[https://docs.atlassian.com/bitbucket-server/rest/5.7.0/bitbucket-rest.html REST API]].
  */
object Requests {

  sealed trait Request {
    implicit val defaultJsonFormats = DefaultFormats

    def toJson(): String =
      Serialization.write(this)
  }

  final case class Project(key: String, name: String, description: Option[String]) extends Request

  final case class Repository(name: String, scmId: String, forkable: Boolean) extends Request

  def apply(p: Responses.Project) =
    Project(p.key, p.name, p.description)

  def apply(r: Responses.Repository) =
    Repository(r.name, r.scmId, r.forkable)
}
