package mrmann.scala.nntp

//import scala.actors.Futures._
import com.twitter.util.{Promise, Future}
import response.GroupSelected

/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 7:15 PM
 */
object Main {
  def main(args: Array[String]) {
    val nntp: NNTP = NNTP(
      host = "news.astraweb.com",
      username = "Mrmann87",
      password = "toby1234"
    )
    println(nntp.help().get())
    val group = nntp.group("comp.lang.ruby").get()
    println(group)
    group match {
      case GroupSelected(_, first, _, _) => {
        println(nntp.article(first).get())
      }
    }
    println(nntp.close().get())
  }
}
