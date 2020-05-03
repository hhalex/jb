package workspace

import workspace.config.{VersionedApplication, VersionedBundler, VersionedLanguage}

case class WorkspaceConfig()
case class WorkspaceConfig2(a: VersionedApplication, l: VersionedLanguage, b: VersionedBundler, t: String)
