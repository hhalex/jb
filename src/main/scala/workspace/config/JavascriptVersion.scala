package workspace.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

sealed trait JavascriptVersion
sealed trait InputJavascriptVersion

object JavascriptVersion {
  case object ES3 extends JavascriptVersion
  case object ES5 extends JavascriptVersion
  case object ES6 extends JavascriptVersion with InputJavascriptVersion
  case object ES7 extends JavascriptVersion with InputJavascriptVersion
  case object ES2015 extends JavascriptVersion with InputJavascriptVersion
  case object ES2016 extends JavascriptVersion with InputJavascriptVersion
  case object ES2017 extends JavascriptVersion with InputJavascriptVersion
  case object ES2018 extends JavascriptVersion with InputJavascriptVersion
  case object ES2019 extends JavascriptVersion with InputJavascriptVersion
  def validate(s: String): Validated[String, JavascriptVersion] = s.toLowerCase match {
    case "es3" => Valid(ES3)
    case "es5" => Valid(ES5)
    case "es6" => Valid(ES6)
    case "es7" => Valid(ES7)
    case "es2015" => Valid(ES2015)
    case "es2016" => Valid(ES2016)
    case "es2017" => Valid(ES2017)
    case "es2018" => Valid(ES2018)
    case unknowJsVersion => Invalid(s"'$unknowJsVersion' is not a valid javascript version.")
  }

  def validateInput(jsVersion: JavascriptVersion) = jsVersion match {
    case v => Invalid(s"'$v' is not an accepted javascript version as an input for bundling (> es5)")
    case v: InputJavascriptVersion => Valid(v)
  }
}
