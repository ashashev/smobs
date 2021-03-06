/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

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

  final case class Project(key: String, name: String, description: Option[String], public: Boolean) extends Request

  final case class Repository(name: String, scmId: String, forkable: Boolean, public: Boolean) extends Request

  def apply(p: Responses.Project) =
    Project(p.key, p.name, p.description, p.public)

  def apply(r: Responses.Repository) =
    Repository(r.name, r.scmId, r.forkable, r.public)
}
