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

sealed trait VersionedLanguage

object VersionedLanguage {
  case class Typescript(version: String) extends VersionedLanguage {
    override def toString: String = s"ts-$version"
  }
  case class Javascript(version: InputJavascriptVersion) extends VersionedLanguage {
    override def toString: String = s"js-$version"
  }

  def validateInput(l: Language, v: String) = l match {
    case Language.Typescript => NpmVersion.validate(v).map(Typescript)
    case Language.Javascript => JavascriptVersion.validate(v).andThen(JavascriptVersion.validateInput).map(Javascript)
    case _ => Invalid(s"Can't validate version '$v' for unknown language '$l'")
  }
}