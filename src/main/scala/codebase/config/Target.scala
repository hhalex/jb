package codebase.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

sealed trait Target
case object Es3 extends Target
case object Es5 extends Target
case object Es6 extends Target

object Target {
  def apply(ts: String): Validated[String, Target] = ts match {
    case "es3" => Valid(Es3)
    case "es5" => Valid(Es5)
    case "es6" => Valid(Es6)
    case s => Invalid(s"Unable to identify target $s")
  }
}
