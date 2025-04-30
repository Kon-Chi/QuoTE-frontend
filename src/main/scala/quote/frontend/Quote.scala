package quote.frontend

import mhtml.*
import scala.xml.Node
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*
import org.scalajs.dom.*
import scala.collection.immutable.Queue

import quote.ot.*

object Main {
  val wsClient = WebSocketClient("shareddocument")
  var lastText: String = ""
  val dmp = DiffMatchPatch()
  val editor = TextArea("", onInput)

  def updateText(s: String) =
    lastText = s
    editor.text := s
    println(s"UPDT: $s")

  def initWebSocket(): Unit = {
    wsClient.connect()

    wsClient.setOnDocumentReceived { doc =>
      lastText = doc
      editor.text := doc
      println(s"Received $doc")
    }

    wsClient.setOnDocumentUpdate { ops =>
      var s = ""
      editor.text.update(text => {
        val newText = ops.foldLeft(text)((t, op) => applyOperation(op, t))
        updateText(newText)
        s = newText
        newText
      })
      println(s"Updated -> $s")
    }
  }

  private def applyOperation(op: Operation, doc: String): String = {
    println(s"!!!${doc.length} $doc")
    op match {
      case Insert(index, str) =>
        if (0 <= index && index <= doc.length) {
          doc.take(index) + str + doc.drop(index)
        } else {
          throw IndexOutOfBoundsException("Insertion into an invalid position")
        }
      case Delete(index, len) =>
        if (0 <= index && index < doc.length) {
          doc.take(index) + doc.drop(index + len)
        } else {
          throw IndexOutOfBoundsException("Deletion from an invalid position")
        }
      case null => doc
    }
  }

  def handleUserAction(oldText: String, newText: String): Unit = {
    println(s"$oldText -> $newText")
    val ops = calculateTextDiff(oldText, newText)
    ops.foreach(dom.console.log(_))
    wsClient.sendLocalOperations(ops.toList)
  }

  def calculateTextDiff(
      oldText: String,
      newText: String
  ): Queue[Operation] = {
    val diffs = dmp.diff_main(lastText, newText)
    dmp.diff_cleanupSemantic(diffs)

    diffs.foldLeft((0, Queue[Operation]())) { (acc, diff) =>
      var (pos, ops) = acc
      val (opType, text) =
        (diff(0).asInstanceOf[Int], diff(1).asInstanceOf[String])

      if opType == 1 then ops = ops.appended(Insert(pos, text))
      else if opType == -1 then
        ops = ops.appended(Delete(pos, text.length))

      (pos + text.length, ops)
    }
  }._2

  def onInput(e: InputEvent): Unit =
    val newText = e.target.asInstanceOf[html.TextArea].value
    handleUserAction(lastText, newText)
    updateText(newText)

  def main(args: Array[String]): Unit = {
    initWebSocket()
    val div = dom.document.getElementById("app")
    mount(div, editor.component)
  }
}
