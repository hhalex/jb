package workspace.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object JavascriptVersion {
  def validate(s: String): Validated[String, String] =
    if (s.matches("es3|es5|es6|es7|es2015|es2016|es2017|es2018"))
      Valid(s)
    else
      Invalid(s"'$s' is not a valid javascript version.")
}
