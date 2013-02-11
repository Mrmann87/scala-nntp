package mrmann.scala.nntp.response

/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 8:21 PM
 */
abstract class CommandResponse()

case class Article(text: String) extends CommandResponse()

case class Body(text: String) extends CommandResponse()

case class Head(text: String) extends CommandResponse()

case class ClosingConnection(text: String) extends CommandResponse()

case class Ok(text: String) extends CommandResponse()

case class Help(text: String) extends CommandResponse()

case class GroupSelected(numArticles: Long,
                         firstArticle: Long,
                         lastArticle: Long,
                         groupName: String) extends CommandResponse

