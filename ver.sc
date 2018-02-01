import sys.process._

val described = Seq("git", "describe", "--match", "v*").!!

val full_version_r = """v([0-9]+\.[0-9]+)\.([0-9]+)""".r
val short_version_r = """v([0-9]+\.[0-9]+)""".r
val untagged_full_version_r = """v([0-9]+\.[0-9]+)\.([0-9]+)-([0-9]+)-g(.+)""".r
val untagged_short_version_r = """v([0-9]+\.[0-9]+)-([0-9]+)-g(.+)""".r

val tag = described.trim match {
    case full_version_r(ver, minor) => ver + "." + minor
    case short_version_r(ver) => ver + ".0"
    case untagged_full_version_r(ver, minor, patch, commit) => s"$ver.${minor.toInt+patch.toInt}"
    case untagged_short_version_r(ver, patch, commit) => s"$ver.$patch"
    case x => "v0.0.0"
}

println(tag)
