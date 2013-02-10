package mrmann.scala.nntp

import errors.Exceptions.NNTPCommandException
import java.io.{InputStreamReader, PrintWriter, BufferedReader, Closeable}
import java.net.Socket
import response._
import response.GroupSelectedResponse


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
            numConnections: Int = 1) = new NNTP(host, port, username, password, numConnections)
}

class NNTP(val host: String,
           val port: Int,
           val username: String,
           val password: String,
           val numConnections: Int) extends Closeable {
  private var socket: Socket = null
  private var incoming: BufferedReader = null
  private var outgoing: PrintWriter = null

  private var connected: Boolean = false
  private var postingAllowed: Boolean = false

  require(host != null && !host.isEmpty, "host is required")

  connect()
  login()

  def isConnected = connected

  def connect() {
    println("Connecting")
    socket = new Socket(host, port)
    incoming = new BufferedReader(new InputStreamReader(socket.getInputStream))
    outgoing = new PrintWriter(socket.getOutputStream)
    println("Connected")
    // Handle the
    NNTPResponseFactory.status(incoming) match {
      case response@StatusResponse(code, text) => {
        println("Got response: " + response)
        postingAllowed = code match {
          case 200 => true
          case 201 => false
          case _ => throw new NNTPCommandException(code, text)
        }
        connected = true
      }
    }
  }

  def close() {
    if (socket != null) {
      println("Closing socket")
      connected = false
      socket.close()
    }
  }

  def help(): HelpResponse = {
    write("HELP") match {
      case response@StatusResponse(code, text) => {
        println("Got response: " + response)
        code match {
          case 100 => NNTPResponseFactory.text(incoming) match {
            case response@TextResponse(`text`) => HelpResponse(text.mkString("\n"))
          }
          case _ => throw new NNTPCommandException(code, text)
        }
      }
    }
  }

  def group(name: String): GroupSelectedResponse = {
    write("GROUP", name) match {
      case response@StatusResponse(code, text) => {
        println("Got response: " + response)
        code match {
          case 211 => {
            val pieces = text.split(" ")
            GroupSelectedResponse(
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

  def login(): NNTPResponse = {
    write("AUTHINFO", "USER", username) match {
      case response@StatusResponse(code, text) => {
        println("Got response: " + response)
        code match {
          case 381 => {
            // 381 Password required
            write("AUTHINFO", "PASS", password) match {
              case response@StatusResponse(`code`, `text`) => {
                println("Got response: " + response)
                code match {
                  case 281 => response // 281 Authentication Accepted
                  case _ => throw new NNTPCommandException(code, text) // 481 Authentication failed or 502 Command unavailable
                }
              }
            }
          }
          case 281 => response // 281 Authentication Accepted
          case _ => throw new NNTPCommandException(code, text) // 481 Authentication failed or 502 Command unavailable
        }
      }
    }
  }

  private def write(commandArgs: Any*): NNTPResponse = {
    val command = commandArgs.mkString(" ") + "\r\n"
    println("Writing command: " + command)
    outgoing.print(command)
    outgoing.flush()
    NNTPResponseFactory.status(incoming)
  }
}
