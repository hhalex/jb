package com.hhalex.jb

import java.nio.file.{Paths}

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
import workspace.{Codebase, Workspace, WorkspaceIO, WorkspaceMatcher}

object Main extends IOApp {
  def run(args: List[String]) = {

    val blocker = Blocker[IO]

    val app = HttpRoutes
      .of[IO] {
            
        case request@(Method.POST -> Root / "bundle" :? WorkspaceMatcher(vw)) => {
            vw match {
              case Valid(w) => {
                Ok(for {
                  b <- Stream.resource(blocker)
                  res <- Stream.eval(IO(s"npm run --silent rollup --prefix ${w.path.toString}".!!))
                } yield res)
              }
              case Invalid(nel) => BadRequest(nel.mkString("\n"))
            }
        }
        case request@(Method.POST -> Root / "workspace" :? WorkspaceMatcher(vw)) => {
          blocker.use {b => {
            request.decode[Multipart[IO]] { m => {
              (vw, Codebase.extract(m).toValidatedNel).tupled match {
                case Valid((workspace, codebaseFile)) => {
                  val untarCodebase: IO[Unit] = codebaseFile
                    .through(archive.untar(512))
                    .flatMap({
                      case (filePath, s) => {
                        val file = workspace.path.resolve(filePath)
                        for {
                          _ <- Stream.eval(io.file.createDirectories[IO](b, workspace.path))
                          _ <- s.through(io.file.writeAll(file, b)).handleError(e => println(e.getStackTrace.mkString("\n")))
                        } yield ()
                      }
                    })
                    .compile
                    .drain

                  Ok(for {
                    _ <- untarCodebase
                    _ <- WorkspaceIO.initConfigFiles(workspace, b).compile.drain
                    _ <- IO(s"npm install --prefix ${workspace.path.toString}".!!)
                  } yield ())
                }
                case Invalid(errorList) => Ok(errorList.foldMap(_.toString))
              }
            }}
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