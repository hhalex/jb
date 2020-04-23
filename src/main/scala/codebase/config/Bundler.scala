package codebase.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

sealed trait Bundler
case object Rollup extends Bundler

object Bundler {
  def apply(s: String): Validated[String, Bundler] = s match {
    case "rollup" => Valid(Rollup)
    case s => Invalid(s"Unable to identify bundler $s")
  }
}

case class BundlerVersion(b: Bundler, version: String)