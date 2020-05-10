package workspace.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import workspace.config.Language.{Javascript, Typescript}

sealed trait Language

object Language {
  case object Typescript extends Language
  case object Javascript extends Language
  def apply(ts: String): Validated[String, Language] = ts match {
    case "ts" => Valid(Typescript)
    case "js" => Valid(Javascript)
    case s => Invalid(s"Unknown language '$s'")
  }
}

case class VersionedLanguage(l: Language, version: String)

object VersionedLanguage {
  def validate(l: Language, v: String) = l match {
    case Typescript => NpmVersion.validate(v).map(validVersion =>  VersionedLanguage(Typescript, validVersion))
    case Javascript => JavascriptVersion.validate(v).map(validVersion => VersionedLanguage(Javascript, validVersion))
    case _ => Invalid(s"Can't validate version '$v' for unknown language '$l'")
  }
}