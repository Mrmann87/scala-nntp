package mrmann.scala.nntp.response

/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 5:37 PM
 */
abstract class NNTPResponse()

object StatusResponse {
  def apply(code: Int, text: String) = new StatusResponse(code, text)

  def unapply(r: StatusResponse): Option[(Int, String)] = Some(r.code, r.text)
}

class StatusResponse(val code: Int, val text: String) extends NNTPResponse {
  override def toString() = {
    "StatusResponse(%d, %s)".format(code, text)
  }
}

object TextResponse {
  def apply(text: List[String]) = new TextResponse(text)

  def unapply(r: TextResponse): Option[List[String]] = Some(r.text)
}

class TextResponse(val text: List[String]) extends NNTPResponse {
  override def toString() = {
    "TextResponse(\n" +
      text.map("\t%s\n".format(_)).mkString("") +
      ")"
  }
}
