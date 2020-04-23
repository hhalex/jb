package codebase.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

sealed trait Language
case object Typescript extends Language
case object Javascript extends Language

object Language {
  def apply(ts: String): Validated[String, Language] = ts match {
    case "ts" => Valid(Typescript)
    case "js" => Valid(Javascript)
    case s => Invalid(s"Unable to identify language $s")
  }
}

case class LanguageVersion(l: Language, version: String)