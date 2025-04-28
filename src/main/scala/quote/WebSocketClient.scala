import org.scalajs.dom
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import scala.concurrent.Future
import scala.scalajs.js.typedarray._

case class Operation(content: String) // Define actual Operation type
case class ClientInput(revision: Int, operations: List[Operation])
case class ServerUpdate(revision: Int, operations: List[Operation])

object WebSocketClient {
  implicit val operationCodec: Codec[Operation] = deriveCodec[Operation]
  implicit val clientInputCodec: Codec[ClientInput] = deriveCodec[ClientInput]
  implicit val serverUpdateCodec: Codec[ServerUpdate] = deriveCodec[ServerUpdate]

  private var socket: Option[dom.WebSocket] = None
  private var currentState: ClientState = ClientState.initial
  private var currentRevision: Int = 0

  private var onUpdate: List[Operation] => Unit = _ => ()
  private var onError: String => Unit = _ => ()

  def connect(url: String): Future[Unit] = {
    val promise = scala.concurrent.Promise[Unit]()
    
    val ws = new dom.WebSocket(s"ws://${dom.window.location.host}/updates")
    socket = Some(ws)

    ws.onopen = { _: dom.Event =>
      println("WebSocket connected")
      promise.success(())
    }

    ws.onerror = { _: dom.Event =>
      val error = "WebSocket error"
      println(error)
      onError(error)
      promise.failure(new Exception(error))
    }

    ws.onmessage = { event: dom.MessageEvent =>
      event.data match {
        case blob: dom.Blob =>
          val reader = new dom.FileReader()
          reader.onload = { _ =>
            handleMessage(reader.result.asInstanceOf[String])
          }
          reader.readAsText(blob)
        case text: String =>
          handleMessage(text)
        case _ =>
          onError("Unknown message format")
      }
    }

    ws.onclose = { _: dom.Event =>
      println("WebSocket closed")
      socket = None
    }

    promise.future
  }

  private def handleMessage(message: String): Unit = {
    parser.decode[ServerUpdate](message) match {
      case Right(update) =>
        currentRevision = update.revision
        val (transformedOps, newState) = Client.applyServer(currentState, update.operations)
        currentState = newState
        onUpdate(transformedOps)
        
      case Left(error) =>
        onError(s"Failed to parse server update: $error")
    }
  }

  def sendOperations(operations: List[Operation]): Unit = {
    val (shouldSend, newState) = Client.applyClient(currentState, operations)
    currentState = newState
    
    if (shouldSend) {
      val input = ClientInput(currentRevision, operations)
      socket.foreach(_.send(input.asJson.noSpaces))
    }
  }

  def handleServerAck(): Unit = {
    val (opsToSend, newState) = Client.serverAck(currentState)
    currentState = newState
    
    opsToSend.foreach { ops =>
      val input = ClientInput(currentRevision, ops)
      socket.foreach(_.send(input.asJson.noSpaces))
    }
  }

  def setCallbacks(
    updateHandler: List[Operation] => Unit,
    errorHandler: String => Unit
  ): Unit = {
    onUpdate = updateHandler
    onError = errorHandler
  }

  def close(): Unit = {
    socket.foreach(_.close())
    socket = None
  }
}