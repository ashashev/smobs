name := "smobs"
organization := "com.github.ashashev"
scalaVersion := "2.12.4"

enablePlugins(GitVersioning, GitBranchPrompt)
git.useGitDescribe := true

val full_version_r = """v([0-9]+\.[0-9]+)\.([0-9]+)""".r
val short_version_r = """v([0-9]+\.[0-9]+)""".r
val untagged_full_version_r = """v([0-9]+\.[0-9]+\.[0-9]+)-([0-9]+)-g(.+)""".r
val untagged_short_version_r = """v([0-9]+\.[0-9]+)-([0-9]+)-g(.+)""".r

git.gitTagToVersionNumber := { tag: String =>
  println("tag: " + tag)
  val ver = tag match {
    case full_version_r(ver, minor) => Some(ver + "." + minor)
    case short_version_r(ver) => Some(ver + ".0")
    case untagged_full_version_r(ver, patch, commit) => Some(s"$ver-$patch ($commit)")
    case untagged_short_version_r(ver, patch, commit) => Some(s"$ver-$patch ($commit)")
    case _ => Some(tag)
  }
  println("ver: " + ver.getOrElse("unknown"))
  ver
}

scalacOptions += "-unchecked"
scalacOptions += "-deprecation"
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.github.scopt" %% "scopt" % "3.7.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-native" % "3.5.0"
)
