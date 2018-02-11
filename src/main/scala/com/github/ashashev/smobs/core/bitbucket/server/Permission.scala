package com.github.ashashev.smobs.core.bitbucket.server

import org.json4s._

sealed trait Permission {
  val value: String

  def unapply(arg: String): Boolean = arg == value

  override def toString: String = s"Permission($value)"
}

object Permission {

  def apply(permission: String): Permission = permission match {
    case SysAdmin() => SysAdmin
    case Admin() => Admin
    case ProjectCreate() => ProjectCreate
    case LicensedUser() => LicensedUser
    case ProjectAdmin() => ProjectAdmin
    case ProjectWrite() => ProjectWrite
    case ProjectRead() => ProjectRead
    case RepoAdmin() => RepoAdmin
    case RepoWrite() => RepoWrite
    case RepoRead() => RepoRead
    case _ => throw new Error(s"The permission '$permission' is unknown.")
  }

  final object SysAdmin extends Permission {
    val value = "SYS_ADMIN"
  }

  final object Admin extends Permission {
    val value = "ADMIN"
  }

  final object ProjectCreate extends Permission {
    val value = "PROJECT_CREATE"
  }

  final object LicensedUser extends Permission {
    val value = "LICENSED_USER"
  }

  final object ProjectAdmin extends Permission {
    val value = "PROJECT_ADMIN"
  }

  final object ProjectWrite extends Permission {
    val value = "PROJECT_WRITE"
  }

  final object ProjectRead extends Permission {
    val value = "PROJECT_READ"
  }

  final object RepoAdmin extends Permission {
    val value = "REPO_ADMIN"
  }

  final object RepoWrite extends Permission {
    val value = "REPO_WRITE"
  }

  final object RepoRead extends Permission {
    val value = "REPO_READ"
  }

}

object PermissionSerializer extends CustomSerializer[Permission](
  format => ( {
    case JString(s) => Permission(s)
  }, {
    case p: Permission => JString(p.value)
  }
  )) {

}