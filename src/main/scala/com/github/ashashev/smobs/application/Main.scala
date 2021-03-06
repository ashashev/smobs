/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.application.Git.Credential

object Main extends App {
  Params.parse(args, Params("smobs.json")) match {
    case Some(Params(_, _, true)) =>
      println(Config.toJson(Config()))
    case Some(Params(Config(config), true, _)) => //not migrate
      val migrator = MigratorProjects(config, FakeProcessing)
      migrator.travers()
      FakeProcessing.printStatistics()
    case Some(Params(Config(config), false, _)) =>
      val migrator = MigratorProjects (config, DefaultProcessing(
        Git(if (config.useCredential) {
          Some(Credential("git-credential", Set(config.source, config.destination)))
        } else None),
        config.copyPermissions
      ))
      migrator.travers()
    case _ =>
      System.exit(1)
  }
}
