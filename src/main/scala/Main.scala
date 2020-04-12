package exystence


import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {
  def run(args: List[String]) = {

    val app = HttpRoutes
      .of[IO] {
        case Method.GET -> Root  => Ok("coucou")
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