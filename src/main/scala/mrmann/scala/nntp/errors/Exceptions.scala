package mrmann.scala.nntp.errors

/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 6:12 PM
 */

object Exceptions {
  class NNTPSocketException(message: String) extends RuntimeException(message)
  class NNTPCommandException(code: Int, message: String)extends RuntimeException(code + " " + message)
}

