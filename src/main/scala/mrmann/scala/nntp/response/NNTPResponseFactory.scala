package mrmann.scala.nntp.response

import java.io.BufferedReader
import mrmann.scala.nntp.errors.Exceptions.NNTPSocketException

/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 5:56 PM
 */
object NNTPResponseFactory {
  def status(incoming: BufferedReader): StatusResponse = {
    val line = incoming.readLine().trim
    if (!line.isEmpty) {
      val (code, text) = line.splitAt(3)
      StatusResponse(code.toInt, text.trim)
    } else throw new NNTPSocketException("Empty response")
  }

  def text(incoming: BufferedReader): TextResponse = {
    TextResponse(collect(incoming, Nil))
  }

  private def collect(incoming: BufferedReader, lines: List[String]): List[String] = {
    val line = incoming.readLine().trim
    if (line.charAt(0) != '.')
      collect(incoming, line :: lines)
    else if (line.length > 1 && line.charAt(1) == '.')
      collect(incoming, line.substring(1) :: lines)
    else lines
  }
}
