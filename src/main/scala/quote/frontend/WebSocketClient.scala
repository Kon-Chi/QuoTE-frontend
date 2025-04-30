package quote.frontend

import org.scalajs.dom
import org.scalajs.dom.{WebSocket, Event, MessageEvent}
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*

import quote.ot.*

case class ServerInitialMessage(
    revision: Int,
    text: String
) derives Codec.AsObject

case class ClientInput(
    revision: Int,
    operations: List[Operation]
) derives Codec.AsObject

type ServerUpdate = List[Operation]

class WebSocketClient() {
  private val documentId: String = {
    val path = dom.window.location.pathname
    path.split('/').filter(_.nonEmpty).lastOption.getOrElse("default")
  }

  private val wsHost: String = "127.0.0.1:8080"
  private val wsUrl = s"ws://$wsHost/updates/$documentId"

  private var ws: WebSocket = _
  private var documentReceived: Boolean = false
  private var clientState: ClientState = ClientState.initial
  private var currentRevision: Int = 0

  private var onDocumentReceived: String => Unit = _ => ()
  private var onDocumentUpdate: List[Operation] => Unit = _ => ()
  private var onServerAck: () => Unit = () => ()
  private var onConnectionChange: Boolean => Unit = _ => ()

  def connect(): Unit = {
    if ws != null then throw RuntimeException("The socket is already connected")

    ws = new WebSocket(wsUrl)

    ws.onopen = (_: Event) => {
      onConnectionChange(true)
      println("WebSocket connected")
    }

    ws.onclose = (_: Event) => {
      onConnectionChange(false)
      println("WebSocket disconnected")
    }

    ws.onerror = (_: Event) => {
      println("WebSocket error")
    }

    ws.onmessage = (event: MessageEvent) => {
      val message = event.data.toString
      if !documentReceived then
        parse(message).flatMap(_.as[ServerInitialMessage]) match {
          case Right(init) =>
            onDocumentReceived(init.text)
            currentRevision = init.revision
            documentReceived = true
          case Left(parsingError) =>
            println(s"Failed to decode server message: $parsingError")
        }
      else if message == "ack" then
        handleServerAck()
        println("Acked")
      else
        parse(message).flatMap(_.as[ServerUpdate]) match {
          case Right(serverUpdate) =>
            handleServerUpdate(serverUpdate)
          case Left(parsingError) =>
            println(s"Failed to decode server message: $parsingError")
        }
    }
  }

  def disconnect(): Unit = {
    if (ws != null) {
      ws.close()
      ws = null
    }
    throw RuntimeException("The socket is already closed")
  }

  def setOnDocumentReceived(callback: String => Unit): Unit = {
    onDocumentReceived = callback
  }

  def setOnDocumentUpdate(callback: List[Operation] => Unit): Unit = {
    onDocumentUpdate = callback
  }

  def setOnServerAck(callback: () => Unit): Unit = {
    onServerAck = callback
  }

  def setOnConnectionChange(callback: Boolean => Unit): Unit = {
    onConnectionChange = callback
  }

  def sendLocalOperations(ops: List[Operation]): Unit = {
    val (shouldSend, newState) = Client.applyClient(clientState, ops)
    clientState = newState

    if (shouldSend) {
      ops.foreach(_ match
        case i @ Insert(_, _) => OperationHistory.putInsertOp(i)
        case d @ Delete(_, _) => OperationHistory.putDeleteOp(d)
      )
      sendOperations(ops)
    }
  }

  private def handleServerAck(): Unit =
    val (maybeOps, ackState) = Client.serverAck(clientState)
    clientState = ackState
    maybeOps.map(ops => {
      sendOperations(ops)
      ops.foreach(_ match
        case i @ Insert(_, _) => OperationHistory.putInsertOp(i)
        case d @ Delete(_, _) => OperationHistory.putDeleteOp(d)
      )
    })
    currentRevision += 1
    onServerAck()

  private def handleServerUpdate(update: ServerUpdate): Unit = {
    val (transformedOps, newState) =
      Client.applyServer(clientState, update)
    clientState = newState
    currentRevision += 1
    // currentRevision = update.revision
    onDocumentUpdate(transformedOps)
  }

  private def sendOperations(ops: List[Operation]): Unit = {
    if (ws.readyState == WebSocket.OPEN) {
      val input = ClientInput(currentRevision, ops)
      ws.send(input.asJson.noSpaces)
    }
  }
}
