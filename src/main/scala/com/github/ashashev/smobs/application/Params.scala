package com.github.ashashev.smobs.application

case class Params(config: String, notMigrate: Boolean = false, showExample: Boolean = false)

object Params extends scopt.OptionParser[Params]("smobs") {
  //private val version = getClass.getPackage.get
  head(getClass.getPackage.getImplementationTitle, getClass.getPackage.getImplementationVersion,
    """
      |A tool for simple migration of bitbucket server repositories to another bitbucket server
      |Copyright (C) 2018 Aleksei Shashev
      |
      |This program is free software: you can redistribute it and/or modify
      |it under the terms of the GNU General Public License as published by
      |the Free Software Foundation, version 3 of the License
      |<https://www.gnu.org/licenses/gpl-3.0.html>.
      |
      |This program is distributed in the hope that it will be useful,
      |but WITHOUT ANY WARRANTY; without even the implied warranty of
      |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      |""".stripMargin)

  help("help").abbr("h").text("prints this usage text")

  version("version").text("shows version")

  opt[String]('c', "config").text("path to the configuration file").action(
    (config, params) => params.copy(config = config)
  )

  opt[Unit]('e', "example").text("outputs an example configuration file").action(
    (_, p) => p.copy(showExample = true)
  )

  opt[Unit]('n', "no-migrate").
    text("not perform migration, just shows that it will migrate").action(
    (_, p) => p.copy(notMigrate = true)
  )
}
