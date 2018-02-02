/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.core.bitbucket.server._

import scala.util.matching.Regex

class ProjectTransformer(includeProjects: Set[Regex],
                         excludeProjects: Set[Regex],
                         includeUsers: Set[Regex],
                         excludeUsers: Set[Regex],
                         prefix: String) {

  import ProjectTransformer._

  def filteredProjects(project: Requests.Project): Boolean = {
    includeProjects.exists(_.findFirstIn(project.name).isDefined) &&
      excludeProjects.forall(_.findFirstIn(project.name).isEmpty)
  }

  def filteredUsers(project: Requests.Project): Boolean = {
    includeUsers.exists(_.findFirstIn(project.name).isDefined) &&
      excludeUsers.forall(_.findFirstIn(project.name).isEmpty)
  }

  def transform(project: Requests.Project,
                existed: Seq[Requests.Project]): (Requests.Project, State) = {
    if (project.key.startsWith("~")) {
      existed.find(_.key == project.key) match {
        case Some(p) => (p, Exist)
        case None => (project, NotExist)
      }
    } else {
      val name = prefix + project.name
      existed.find(_.name == name) match {
        case Some(p) => (p, Exist)
        case None =>
          val keys = existed.map(_.key).toSet
          val key = newKey(project.key, keys, prefix)
          (project.copy(key = key, name = name), NotExist)
      }
    }
  }
}

object ProjectTransformer {

  sealed abstract class State

  final case object Exist extends State

  final case object NotExist extends State

  def apply(config: Config): ProjectTransformer = {
    new ProjectTransformer(
      config.includeProjects.toSet[String].map(_.r),
      config.excludeProjects.toSet[String].map(_.r),
      config.includeUsers.toSet[String].map(_.r),
      config.excludeUsers.toSet[String].map(_.r),
      config.addedProjectPrefix)
  }

  def apply(includeProjects: Set[Regex],
            excludeProjects: Set[Regex],
            includeUsers: Set[Regex],
            excludeUsers: Set[Regex],
            prefix: String): ProjectTransformer =
    new ProjectTransformer(includeProjects,
      excludeProjects,
      includeUsers,
      excludeUsers,
      prefix)

  /**
    * Creates a new key.
    *
    * This method suggests that key contains from the base name and a index.
    * It increments the index if one exists otherwise it adds an index
    * startting at 2.
    *
    * @param key
    * @return
    */
  def nextKey(key: String): String = {
    val regex = "^(.*?)([0-9]+)".r
    key match {
      case regex(base, number) => base + (number.toInt + 1).toString
      case _ => key + "2"
    }
  }

  /**
    * Creates a new key that doesn't exist in the keys.
    *
    * @param key
    * @param keys
    * @param prefix
    * @return
    */
  def newKey(key: String, keys: Set[String], prefix: String): String = {
    def notExists(key: String) = !keys.contains(key)

    def findKey(probe: String): String = {
      if (notExists(probe)) findKey(nextKey(probe))
      else probe
    }

    if (notExists(key)) key
    else if (notExists(prefix + key)) prefix + key
    else findKey(key)
  }
}
