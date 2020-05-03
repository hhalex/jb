package workspace.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object NpmVersion {
  private val regex = "^[0-9]+\\.[0-9]+\\.[0-9]+$"
  def validate(s: String): Validated[String, String] = if (s.matches(regex))
    Valid(s)
  else Invalid(s"'$s' is not a valid npm version.")
}