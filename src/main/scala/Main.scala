package com.hhalex.jb

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.multipart.Multipart
import org.http4s.server.blaze.BlazeServerBuilder
import fs2._

import sys.process._
import cats.data.Validated._
import cats.implicits._
import workspace.{Codebase, InputFilesMatcher, WorkspaceIO, WorkspaceMatcher}

object Main extends IOApp {
  def run(args: List[String]) = {

    val blocker = Blocker[IO]

    val app = HttpRoutes
      .of[IO] {

        case Method.GET -> Root / "bundle" :? WorkspaceMatcher(vw) +& InputFilesMatcher(validatedInputs) => {
          (vw, validatedInputs.toValidatedNel).tupled match {
            case Valid((w, inputs)) => {
              blocker.use { b => {
                WorkspaceIO.checkInputFilesExist(w, b, inputs.toList).flatMap {
                  case Valid(files) => Ok(WorkspaceIO.bundleWithRollup(w, files))
                  case Invalid(missingFiles) => BadRequest(IO.pure(s"File(s) " + missingFiles.map(f => s"'$f'").mkString(", ") + " not found."))
                }
              }}
            }
            case Invalid(errors) => BadRequest(errors.mkString("\n"))
          }
        }
        case request@Method.POST -> Root / "workspace" :? WorkspaceMatcher(vw) => {
            request.decode[Multipart[IO]] { m => {
              (vw, Codebase.extract(m).toValidatedNel).tupled match {
                case Valid((workspace, codebaseFile)) => {
                  def untarCodebase(b: Blocker) = codebaseFile
                    .through(archive.untar(512))
                    .flatMap({
                      case (filePath, s) => {
                        val file = workspace.codebasePath.resolve(filePath)
                        for {
                          _ <- Stream.eval(io.file.createDirectories[IO](b, file.getParent))
                          _ <- s.through(io.file.writeAll(file, b)).handleError(e => println(e.getStackTrace.mkString("\n")))
                        } yield ()
                      }
                    })
                    .compile
                    .drain

                  Ok(for {
                    b <- Stream.resource(blocker)
                    _ <- Stream.eval(untarCodebase(b))
                    _ <- Stream.eval(WorkspaceIO.initConfigFiles(workspace, b).compile.drain)
                    _ <- Stream.eval(IO(s"npm install --prefix ${workspace.rootPath.toString}".!!))
                  } yield ())
                }
                case Invalid(errorList) => BadRequest(errorList.foldMap(_.toString))
              }
            }}
        }
      }
      .orNotFound

    BlazeServerBuilder[IO]
      .withHttpApp(app)
      .bindHttp(8081, "localhost")
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}