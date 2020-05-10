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

sealed trait VersionedBundler

object VersionedBundler {
  case class Rollup(version: String) extends VersionedBundler {
    override def toString: String = s"rollup-$version"
  }

  def validate(b: Bundler, v: String): Validated[String, VersionedBundler] = b match {
    case Bundler.Rollup => NpmVersion.validate(v).map(Rollup)
  }
}