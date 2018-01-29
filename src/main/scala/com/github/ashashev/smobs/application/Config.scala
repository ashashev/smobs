/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import org.json4s._
import org.json4s.native.{JsonMethods, Serialization}

import scala.util.{Failure, Success, Try}

case class Server(url: String = "http://example.com",
                  user: String = "<your login>",
                  password: String = "<your password>")

case class Config(source: Server = Server(),
                  destination: Server = Server(),
                  includeProjects: Seq[String] = Seq(".*"),
                  excludeProjects: Seq[String] = Seq.empty[String],
                  includeUsers: Seq[String] = Seq(".*"),
                  excludeUsers: Seq[String] = Seq.empty[String],
                  addedProjectPrefix: String = ""
                 )

object Config {
  implicit val formats = Serialization.formats(NoTypeHints)

  def unapply(pathToFile: String): Option[Config] = {
    val in = new java.io.File(pathToFile)
    if (in.isFile() && in.exists()) {
      val is = FileInput(in)
      Try(JsonMethods.parse(is)).map(_.extract[Config]) match {
        case Success(config) => Some(config)
        case Failure(e) => Console.err.println(s"Can't load configuration from '$pathToFile'")
          Console.err.println(e.getMessage)
          None
      }
    } else {
      Console.err.println(s"The '$pathToFile' isn't file or doesn't exist.")
      None
    }
  }

  def toJson(cfg: Config): String = {
    Serialization.writePretty(cfg)
  }
}