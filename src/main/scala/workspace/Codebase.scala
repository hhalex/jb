package workspace

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import fs2.Stream
import org.http4s.multipart.Multipart

object Codebase {
  private def extractFile(m: Multipart[IO], fileName: String): Validated[String, Stream[IO, Byte]] =
      m.parts.find(_.filename.contains(fileName)).map(_.body) match {
        case Some(value) => Valid(value)
        case None => Invalid(s"File '$fileName' is missing.")
      }
  def extract(m: Multipart[IO]): Validated[String, Stream[IO, Byte]] = extractFile(m, "codebase.tar")
}
