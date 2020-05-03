package com.hhalex.jb

import java.nio.file.{Paths, StandardOpenOption}

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.multipart.Multipart
import org.http4s.server.blaze.BlazeServerBuilder
import fs2._

import sys.process._
import cats.data._
import cats.data.Validated._
import cats.implicits._
import workspace.config.{Bundler, JavascriptVersion, Language, VersionedBundler, VersionedLanguage}
import workspace.{WorkspaceConfig, WorkspaceConfig2, WorkspaceIO, WorkspaceMatcher}

trait Read[A] {
  def read(s: String): Option[A]
}

object Read {
  def apply[A](implicit A: Read[A]): Read[A] = A

  implicit val stringRead: Read[String] =
    new Read[String] { def read(s: String): Option[String] = Some(s) }

  implicit val intRead: Read[Int] =
    new Read[Int] {
      def read(s: String): Option[Int] =
        if (s.matches("-?[0-9]+")) Some(s.toInt)
        else None
    }
}

object PushCodebaseValidator {
  sealed trait PushCodebaseRequestValidation
  case class ParseError(str: String) extends PushCodebaseRequestValidation
  case class MissingField(field: String) extends PushCodebaseRequestValidation
  case class MissingFile(fileName: String) extends PushCodebaseRequestValidation
  type ValidationResult[A] = Validated[PushCodebaseRequestValidation, A]

  private def extractField[T: Read](m: Multipart[IO], fieldName: String): ValidationResult[T] = {
    val optValue = for {
      part <- m.parts.find(_.name.contains(fieldName))
      value <- part.body.through(fs2.text.utf8Decode).compile.toList.unsafeRunSync().headOption
    } yield value

    optValue match {
      case Some(strValue) => Read[T].read(strValue) match {
        case Some(v) => Valid(v)
        case None => Invalid(ParseError(strValue))
      }
      case None => Invalid(MissingField(fieldName))
    }
  }

  private def extractFile(m: Multipart[IO], fileName: String): ValidationResult[Stream[IO, Byte]] =
    m.parts.find(_.filename.contains(fileName)).map(_.body) match {
      case Some(value) => Valid(value)
      case None => Invalid(MissingFile(fileName))
    }

  def validateRequest(m: Multipart[IO]): ValidatedNec[PushCodebaseRequestValidation, PushCodebaseRequest] =
    (
      extractFile(m, "codebase.tar").toValidatedNec,
      extractField[String](m, "app").toValidatedNec,
      extractField[String](m, "rev").toValidatedNec,
      extractField[Int](m, "conf").toValidatedNec,
      extractField[Int](m, "target").toValidatedNec
    ).mapN(PushCodebaseRequest)
}
case class PushCodebaseRequest(codebase: fs2.Stream[IO, Byte], app: String, revision: String, config: Int, target: Int)
case class BundleRequest(app: String, version: String)

object Main extends IOApp {
  def run(args: List[String]) = {

    val blocker = Blocker[IO]


    val app = HttpRoutes
      .of[IO] {

        case request@(Method.GET -> Root / "q" :? WorkspaceMatcher(w)) => {
          w match {
            case Valid(config) => Ok(config.toString)
            case Invalid(e) => Ok(e.mkString("\n"))
          }
        }

        case request@(Method.POST -> Root / "bundle") => {
          blocker.use {b => {
            request.decode[Multipart[IO]] { m => {
              PushCodebaseValidator.validateRequest(m) match {
                case Valid(a) => {
                  val root = s"./codebases/${a.app}/${a.revision}/${a.config}/${a.target}"
                  Ok(for {
                    res <- IO(s"npm run --silent rollup --prefix $root".!!)
                  } yield res)
                }
                case Invalid(errorList) => Ok(errorList.foldMap(_.toString))
              }
            }}
          }}
        }
        case request@(Method.POST -> Root / "workspace") => {
          blocker.use {b => {
            request.decode[Multipart[IO]] { m => {
              PushCodebaseValidator.validateRequest(m) match {
                case Valid(a) => {
                  val untarCodebase: IO[Unit] = a.codebase
                    .through(archive.untar(512))
                    .flatMap({
                      case (filePath, s) => {
                        val file = Paths.get(s"codebases/${a.app}/${a.revision}/${a.config}/${a.target}/$filePath")
                        val parent = file.getParent

                        for {
                          _ <- Stream.eval(io.file.createDirectories[IO](b, parent))
                          _ <- s.through(io.file.writeAll(file, b)).handleError(e => println(e.getStackTrace.mkString("\n")))
                        } yield ()
                      }
                    })
                    .compile
                    .drain

                  Ok(for {
                    _ <- untarCodebase
                    _ <- WorkspaceIO.initConfigFiles(Paths.get(s"codebases/${a.app}/${a.revision}/${a.config}/${a.target}"), b, WorkspaceConfig()).compile.drain
                    _ <- IO.pure(s"npm install --prefix ./codebases/${a.app}/${a.revision}/${a.config}/${a.target}".!!)
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