package workspace

import cats.data.Validated.{Invalid, Valid}
import workspace.config.{Bundler, JavascriptVersion, Language, VersionedApplication, VersionedBundler, VersionedLanguage}
import cats.implicits._

object WorkspaceMatcher {
  def extractString(params: Map[String, collection.Seq[String]], fieldName: String) = {
    params.get(fieldName) match {
      case Some(values) => values match {
        case h :: Nil => Valid(h)
        case Nil => Invalid(s"'$fieldName' is a required query parameter, it should contain a value.")
        case l => Invalid(s"'$fieldName' contains ${l.length} values instead of only one.")
      }
      case None => Invalid(s"'$fieldName' is a required query parameter")
    }
  }

  def unapply(params: Map[String, collection.Seq[String]]) = Some {

    val e = (pName: String) => extractString(params, pName).toValidatedNel

    val versionedApp = (e("app"), e("appRev")).mapN { VersionedApplication }
    val versionedBundler = (e("bundler"), e("bundlerRev")).tupled
      .andThen {
        case (bundler, bundlerRev) =>
          Bundler(bundler).toValidatedNel
            .andThen {
              (b: Bundler) => VersionedBundler.validate(b, bundlerRev).toValidatedNel
            }
      }
    val versionedLanguage = (e("language"), e("languageRev")).tupled
      .andThen {
        case (language, languageRev) =>
          Language(language).toValidatedNel
            .andThen {
              (l: Language) => VersionedLanguage.validate(l, languageRev).toValidatedNel
            }
      }
    val target = e("target") andThen { t => JavascriptVersion.validate(t).toValidatedNel }

    (versionedApp, versionedLanguage, versionedBundler, target).mapN { Workspace }
  }
}
