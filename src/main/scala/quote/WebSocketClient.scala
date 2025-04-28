package quote

import org.scalajs.dom
import org.scalajs.dom.{WebSocket, Event, MessageEvent}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

import quote.ot.*

case class ClientInput(
  revision: Int,
  operations: List[Operation]
) derives Encoder.AsObject, Decoder

case class ServerUpdate(
  revision: Int,
  operations: List[Operation]
) derives Encoder.AsObject, Decoder

class WebSocketClient(documentId: String) {
  private var ws: WebSocket = _
  private var clientState: ClientState = ClientState.initial
  private var currentRevision: Int = 0
  
  private var onDocumentUpdate: String => Unit = _ => ()
  private var onConnectionChange: Boolean => Unit = _ => ()
  
  def connect(): Unit = {
    val wsUrl = s"ws://${dom.window.location.host}/updates?doc=$documentId"
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
    }
  }
  
  def setOnDocumentUpdate(callback: String => Unit): Unit = {
    onDocumentUpdate = callback
  }
  
  def setOnConnectionChange(callback: Boolean => Unit): Unit = {
    onConnectionChange = callback
  }
  
  def applyLocalOperation(op: Operation, currentDoc: String): String = {
    val (shouldSend, newState) = Client.applyClient(clientState, op)
    clientState = newState
    
    if (shouldSend) {
      sendOperations(List(op))
    }
    
    applyOperationToDoc(op, currentDoc)
  }
  
  private def handleServerUpdate(update: ServerUpdate): Unit = {
    val (transformedOps, newState) = 
      Client.applyServer(clientState, update.operations)
    clientState = newState
    currentRevision = update.revision
    
    var currentDoc = ""
    
    transformedOps.foreach { op =>
      currentDoc = applyOperationToDoc(op, currentDoc)
      onDocumentUpdate(currentDoc)
    }
    
    val (maybeOps, ackState) = Client.serverAck(clientState)
    clientState = ackState
    
    maybeOps.foreach { ops =>
      sendOperations(ops)
    }
  }
  
  private def sendOperations(ops: List[Operation]): Unit = {
    if (ws.readyState == WebSocket.OPEN) {
      val input = ClientInput(currentRevision, ops)
      ws.send(input.asJson.noSpaces)
    }
  }
  
  private def applyOperationToDoc(op: Operation, doc: String): String = {
    op match {
      case Insert(index, str) =>
        if (index >= 0 && index <= doc.length) {
          doc.take(index) + str + doc.drop(index)
        } else {
          doc
        }
      case Delete(index, len) =>
        if (index >= 0 && index < doc.length) {
          doc.take(index) + doc.drop(index + len)
        } else {
          doc
        }
      case null => doc
    }
  }
}