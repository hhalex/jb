package workspace

import java.nio.file.Paths

import workspace.config.{JavascriptVersion, VersionedApplication, VersionedBundler, VersionedLanguage}

case class Workspace(vApp: VersionedApplication, vLanguage: VersionedLanguage, vBundler: VersionedBundler, target: JavascriptVersion) {
  val rootPath = Paths.get(s"./workspaces/${vApp.name}/${vApp.rev}/${vBundler.toString}-${vLanguage.toString}-$target")
  val codebasePath = rootPath.resolve("codebase")
}
