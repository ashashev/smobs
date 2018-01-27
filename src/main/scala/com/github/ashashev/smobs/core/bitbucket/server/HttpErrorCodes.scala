package com.github.ashashev.smobs.core.bitbucket.server

/**
  * Expected error codes.
  *
  * For more information, see description of Bitbucket Server
  * [[https://docs.atlassian.com/bitbucket-server/rest/5.7.0/bitbucket-rest.html REST API]].
  */
object HttpErrorCodes {
  def unapply(code: Int): Option[String] = code match {
    case 400 => Some("400 Bad Request")
    case 401 => Some("401 Unauthorized")
    case 403 => Some("403 Forbidden")
    case 404 => Some("404 Not Found")
    case 405 => Some("405 Method Not Allowed")
    case 409 => Some("409 Conflict")
    case 415 => Some("415 Unsupported Media Type")
  }
}
