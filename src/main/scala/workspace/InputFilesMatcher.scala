package workspace

import cats.data.Validated.{Invalid, Valid}

object InputFilesMatcher {
  def unapply(params: Map[String, collection.Seq[String]]) = Some {
    params.get("input") match {
      case None => Invalid("'input' is a required parameter")
      case Some(inputs) => inputs match {
        case Nil => Invalid("'input' parameter exists but has not any value")
        case l => Valid(l)
      }
    }
  }
}
