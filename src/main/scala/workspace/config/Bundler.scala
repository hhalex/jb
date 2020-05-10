package workspace.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

sealed trait Bundler

object Bundler {
  case object Rollup extends Bundler
  def apply(s: String): Validated[String, Bundler] = s match {
    case "rollup" => Valid(Rollup)
    case s => Invalid(s"Unknown bundler '$s'")
  }
}

case class VersionedBundler(b: Bundler, version: String)

object VersionedBundler {
  def validate(b: Bundler, v: String) = b match {
    case Bundler.Rollup => NpmVersion.validate(v).map(validVersion => VersionedBundler(b, validVersion))
  }
}