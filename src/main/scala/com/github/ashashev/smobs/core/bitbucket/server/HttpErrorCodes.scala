/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.core.bitbucket.server

import java.net.HttpURLConnection._

/**
  * Expected error codes.
  *
  * For more information, see description of Bitbucket Server
  * [[https://docs.atlassian.com/bitbucket-server/rest/5.7.0/bitbucket-rest.html REST API]].
  */
object HttpErrorCodes {
  def unapply(code: Int): Option[String] = code match {
    case HTTP_BAD_REQUEST => Some("400 Bad Request")
    case HTTP_UNAUTHORIZED => Some("401 Unauthorized")
    case HTTP_FORBIDDEN => Some("403 Forbidden")
    case HTTP_NOT_FOUND => Some("404 Not Found")
    case HTTP_BAD_METHOD => Some("405 Method Not Allowed")
    case HTTP_CONFLICT => Some("409 Conflict")
    case HTTP_UNSUPPORTED_TYPE => Some("415 Unsupported Media Type")
  }
}
