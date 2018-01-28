package com.github.ashashev.smobs.application

import com.github.ashashev.smobs.core.bitbucket.server._

import scala.util.matching.Regex

class ProjectTransformer(include: Set[Regex], exclude: Set[Regex], prefix: String) {

  import ProjectTransformer._

  def filtered(project: Requests.Project): Boolean = {
    include.exists(_.findFirstIn(project.name).isDefined) &&
      exclude.forall(_.findFirstIn(project.name).isEmpty)
  }

  def transform(project: Requests.Project,
                existed: Seq[Requests.Project]): (Requests.Project, State) = {
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

object ProjectTransformer {

  sealed abstract class State

  final case object Exist extends State

  final case object NotExist extends State

  def apply(config: Config): ProjectTransformer = {
    new ProjectTransformer(config.includeProjects.toSet[String].map(_.r),
      config.excludeProjects.toSet[String].map(_.r),
      config.addedProjectPrefix)
  }

  def apply(include: Set[Regex],
            exclude: Set[Regex],
            prefix: String): ProjectTransformer =
    new ProjectTransformer(include, exclude, prefix)

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
