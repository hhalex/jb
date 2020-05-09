package workspace

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import cats.effect.{Blocker, ContextShift, IO}
import fs2.io

object WorkspaceIO {
  def bundleWithRollup(workspace: Workspace, files: scala.collection.Seq[String]) = {
    import scala.sys.process._

    val path = workspace.rootPath.relativize(workspace.codebasePath)
    val imports = files.map { input => s"""import './$path/$input';""" }.mkString
    val wholeCommand = s"""echo "$imports"""" #| s"npm run rollup --silent --prefix ${workspace.rootPath.toString}"
    IO(wholeCommand.!!)
  }

  def checkInputFilesExist(workspace: Workspace, blocker: Blocker, inputs: scala.collection.Seq[String])(implicit ctx: ContextShift[IO]) =
    inputs.map(input => {
      val p = workspace.codebasePath.resolve(input)
      io.file.exists[IO](blocker, p).map(b => {
        if (b) input.valid
        else input.invalid
      })
    }).toList.sequence.map(all => {
      all.collect({ case Invalid(i) => i }) match {
        case Nil => Right(all.collect({ case Valid(i) => i }))
        case l => Left(l)
      }
    })

  def createPackageJsonFile(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    fs2.Stream.emit("""
      |{
      |  "scripts": {
      |     "rollup": "./node_modules/rollup/dist/bin/rollup --config ./rollup.config.js --format iife"
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
      .through(fs2.io.file.writeAll[IO](workspace.rootPath.resolve("package.json"), blocker))

  def createRollupConfigFile(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    fs2.Stream.emit("""
                      |import typescript from 'rollup-plugin-typescript2';
                      |
                      |export default {
                      |    input: "-",
                      |    plugins: [
                      |      // Compile TypeScript files
                      |      typescript({ useTsconfigDeclarationDir: true })
                      |    ]
                      |  }
                      |""".stripMargin)
      .through(fs2.text.utf8Encode)
      .through(fs2.io.file.writeAll[IO](workspace.rootPath.resolve("rollup.config.js"), blocker))

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
                      |        "codebase/**/*"
                      |        ],
                      |    "exclude": [
                      |        "node_modules",
                      |        "**/*.spec.ts"
                      |    ]
                      |}""".stripMargin)
      .through(fs2.text.utf8Encode)
      .through(fs2.io.file.writeAll[IO](workspace.rootPath.resolve("tsconfig.json"), blocker))


  def initConfigFiles(workspace: Workspace, blocker: Blocker)(implicit ctx: ContextShift[IO]) =
    createPackageJsonFile(workspace, blocker) ++
    createRollupConfigFile(workspace, blocker) ++
      createTsConfigFile(workspace, blocker)
}
