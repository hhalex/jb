package workspace

import java.nio.file.{Path, Paths}

import workspace.config.{VersionedApplication, VersionedBundler, VersionedLanguage}

case class Workspace(a: VersionedApplication, l: VersionedLanguage, b: VersionedBundler, t: String) {
  val path = Paths.get(s"./codebases/${a.name}/${a.rev}/${b.b.toString}-${b.version}-${l.l.toString}-${l.version}-$t")
}
