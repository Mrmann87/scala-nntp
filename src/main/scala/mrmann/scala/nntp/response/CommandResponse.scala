package mrmann.scala.nntp.response

/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 8:21 PM
 */
abstract class CommandResponse()

case class HelpResponse(text: String)

case class GroupSelectedResponse(numArticles: Long,
                         firstArticle: Long,
                         lastArticle: Long,
                         groupName: String) extends CommandResponse

