package workspace

import java.nio.file.Paths

import workspace.config.{VersionedApplication, VersionedBundler, VersionedLanguage}

case class Workspace(a: VersionedApplication, l: VersionedLanguage, b: VersionedBundler, t: String) {
  val rootPath = Paths.get(s"./workspaces/${a.name}/${a.rev}/${b.b.toString}-${b.version}-${l.l.toString}-${l.version}-$t")
  val codebasePath = rootPath.resolve("codebase")
}
