package com.github.ashashev.smobs.application

object Main extends App {
  Params.parse(args, Params("smobs.json")) match {
    case Some(Params(_, _, true)) =>
      println(Config.toJson(Config()))
    case Some(Params(Config(config), true, _)) => //not migrate
      val migrator = MigratorProjects(config, FakeProcessing)
      migrator.traversGeneral()
      if (config.migratePersonalRepositories)
        migrator.traversPersonal()
    case Some(Params(Config(config), false, _)) =>
      ???
    case _ =>
      System.exit(1)
  }
}
