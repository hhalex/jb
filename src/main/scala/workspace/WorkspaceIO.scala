package workspace

import cats.effect.{Blocker, ContextShift, IO}

object WorkspaceIO {
  def createPackageJsonFile(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    fs2.Stream.emit("""
      |{
      |  "scripts": {
      |     "rollup": "./node_modules/rollup/dist/bin/rollup --config ./rollup.config.js"
      |  },
      |  "devDependencies": {
      |    "rollup": "2.2.0",
      |    "rollup-plugin-typescript2": "0.26.0",
      |    "typescript": "3.8.3",
      |    "ts-node": "8.8.1"
      |  }
      |}
      |""".stripMargin)
      .through(fs2.text.utf8Encode)
      .through(fs2.io.file.writeAll[IO](workspace.path.resolve("package.json"), blocker))

  def createRollupConfigFile(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    fs2.Stream.emit("""
                      |import typescript from 'rollup-plugin-typescript2';
                      |
                      |export default {
                      |    input: `src/index.ts`,
                      |    // Indicate here external modules you don't wanna include in your bundle (i.e.: 'lodash')
                      |    external: [],
                      |    plugins: [
                      |      // Compile TypeScript files
                      |      typescript({ useTsconfigDeclarationDir: true })
                      |    ]
                      |  }
                      |""".stripMargin)
      .through(fs2.text.utf8Encode)
      .through(fs2.io.file.writeAll[IO](workspace.path.resolve("rollup.config.js"), blocker))

  def createTsConfigFile(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    fs2.Stream.emit("""
                      |{
                      |    "compilerOptions": {
                      |        "declaration": true,
                      |        "declarationDir": "lib/",
                      |        "outDir": "lib/",
                      |        "module": "es6",
                      |        "noImplicitAny": true,
                      |        "removeComments": true,
                      |        "preserveConstEnums": true,
                      |        "sourceMap": true,
                      |        "target": "ES6"
                      |    },
                      |    "include": [
                      |        "src/**/*"
                      |    ],
                      |    "exclude": [
                      |        "node_modules",
                      |        "**/*.spec.ts"
                      |    ]
                      |}""".stripMargin)
      .through(fs2.text.utf8Encode)
      .through(fs2.io.file.writeAll[IO](workspace.path.resolve("tsconfig.json"), blocker))


  def initConfigFiles(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    createPackageJsonFile(workspace, blocker) ++
    createRollupConfigFile(workspace, blocker) ++
      createTsConfigFile(workspace, blocker)
}
