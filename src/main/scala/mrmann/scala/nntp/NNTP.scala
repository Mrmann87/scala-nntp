package mrmann.scala.nntp

import errors.Exceptions.NNTPCommandException
import java.io.{InputStreamReader, PrintWriter, BufferedReader}
import java.net.Socket
import response._
import response.GroupSelected
import com.twitter.concurrent.Broker
import com.twitter.util.{Promise, Future}


/**
 * User: mrmann
 * Date: 2/9/13
 * Time: 3:50 PM
 */

object NNTP {
  val DEFAULT_PORT = 119

  def apply(host: String,
            port: Int = DEFAULT_PORT,
            username: String = "",
            password: String = "",
            doConnect: Boolean = true,
            doLogin: Boolean = true) = {
    val nntp = new NNTP(host, port, username, password)
    if (doConnect) nntp.connect()
    if (doLogin) nntp.login()
    nntp
  }
}

class NNTP(val host: String,
           val port: Int,
           val username: String,
           val password: String) {

  private var socket: Socket = null
  private var incoming: BufferedReader = null
  private var outgoing: PrintWriter = null

  private var running: Boolean = true
  private var connected: Boolean = false
  private var postingAllowed: Boolean = false

  private val broker = new Broker[() => Unit]
  private val connectPromise: Promise[Unit] = new Promise[Unit]
  private val loginPromise: Promise[Ok] = new Promise[Ok]

  require(host != null && !host.isEmpty, "host is required")

  val thread = new Thread(
    new Runnable {
      def run() {
        try {
          while (running) {
            broker.recv.syncWait()()
          }
        } finally {
          socket.close()
          connected = false
        }
      }
    }
  )
  thread.setDaemon(true)
  thread.start()

  def isConnected = connected

  def connect(): Future[Unit] = {
    val p = connectPromise
    broker ! (() => {
      try {
        println("Connecting")
        socket = new Socket(host, port)
        incoming = new BufferedReader(new InputStreamReader(socket.getInputStream))
        outgoing = new PrintWriter(socket.getOutputStream)
        println("Connected")
        p setValue {
          NNTPResponseFactory.status(incoming) match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              postingAllowed = code match {
                case 200 => true
                case 201 => false
                case _ => throw new NNTPCommandException(code, text)
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  def close(): Future[ClosingConnection] = {
    val p = Promise[ClosingConnection]
    broker ! (() => {
      running = false
      try {
        p setValue {
          write("QUIT") match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              code match {
                case 205 => ClosingConnection(text)
                case _ => throw new NNTPCommandException(code, text)
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  def help(): Future[Help] = {
    val p = new Promise[Help]
    broker ! (() => {
      try {
        p setValue {
          write("HELP") match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              code match {
                case 100 => NNTPResponseFactory.text(incoming) match {
                  case response@TextResponse(text) => Help(text.mkString("\n"))
                }
                case _ => throw new NNTPCommandException(code, text)
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  def group(name: String): Future[GroupSelected] = {
    val p = Promise[GroupSelected]
    broker ! (() => {
      try {
        p setValue {
          write("GROUP", name) match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              code match {
                case 211 => {
                  val pieces = text.split(" ")
                  GroupSelected(
                    pieces(0).toLong,
                    pieces(1).toLong,
                    pieces(2).toLong,
                    pieces(3)
                  )
                }
                case _ => throw new NNTPCommandException(code, text)
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  def login(): Future[Ok] = {
    val p = loginPromise
    broker ! (() => {
      try {
        p setValue {
          write("AUTHINFO", "USER", username) match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              code match {
                case 381 => {
                  // 381 Password required
                  write("AUTHINFO", "PASS", password) match {
                    case response@StatusResponse(code, text) => {
                      println("Got response: " + response)
                      code match {
                        case 281 => Ok(text) // 281 Authentication Accepted
                        case _ => throw new NNTPCommandException(code, text) // 481 Authentication failed or 502 Command unavailable
                      }
                    }
                  }
                }
                case 281 => Ok(text) // 281 Authentication Accepted
                case _ => throw new NNTPCommandException(code, text) // 481 Authentication failed or 502 Command unavailable
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  def stat(number: Long): Future[Unit] = doStat(number)

  def stat(id: String): Future[Unit] = doStat(id)

  private def doStat(param: Any): Future[Unit] = {
    val p = Promise[Unit]
    broker ! (() => {
      try {
        p setValue {
          write("STAT") match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              code match {
                case _ => throw new NNTPCommandException(code, text)
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  def article(number: Long): Future[Article] = doArticle(number)

  def article(id: String): Future[Article] = doArticle(id)

  def article(): Future[Article] = doArticle()

  private def doArticle(param: Any*): Future[Article] = {
    val p = new Promise[Article]
    broker ! (() => {
      try {
        p setValue {
          write(("ARTICLE" +: param):_*) match {
            case response@StatusResponse(code, text) => {
              println("Got response: " + response)
              code match {
                case 220 => NNTPResponseFactory.text(incoming) match {
                  case txtResponse@TextResponse(text) => Article(text.mkString("\n"))
                }
                case _ => throw new NNTPCommandException(code, text)
              }
            }
          }
        }
      } catch {
        case ex => p setException ex
      }
    })
    p
  }

  private def write(commandArgs: Any*): NNTPResponse = {
    val command = commandArgs.mkString(" ") + "\r\n"
    println("Writing command: " + command)
    outgoing.print(command)
    outgoing.flush()
    NNTPResponseFactory.status(incoming)
  }
}
