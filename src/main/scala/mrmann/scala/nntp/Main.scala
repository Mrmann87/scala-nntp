package mrmann.scala.nntp

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
    println(nntp.help())
    println(nntp.group("comp.lang.ruby"))
    nntp.close()
  }
}
