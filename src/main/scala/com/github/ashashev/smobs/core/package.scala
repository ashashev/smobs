/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs

package object core {
  /**
    * Concatenates a slugs in the URL.
    *
    * Doesn't check whether the result is well-formed url.
    *
    * @param slugs
    * @return
    */
  def makeUrl(slugs: String*): String = {
    slugs.map(_.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse).
      mkString("/")
  }
}
