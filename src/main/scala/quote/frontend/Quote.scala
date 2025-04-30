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
import scala.languageFeature.postfixOps

object Main {
  val wsClient = WebSocketClient()
  val dmp = DiffMatchPatch()
  var lastText: String = ""
  val editor = TextArea("", onInput)
  val undoButton = UndoButton(onUndo)
  val redoButton = RedoButton(onRedo)

  def updateText(s: String) =
    lastText = s
    editor.text := s
    println(s"UPDT: $s")

  def initWebSocket(): Unit = {
    wsClient.connect()

    wsClient.setOnDocumentReceived { doc =>
      updateText(doc)
      editor.selection := (doc.length, doc.length)
      println(s"Received $doc")
    }

    wsClient.setOnDocumentUpdate { ops =>
      try
        editor.selection.update(sel => {
          var (selStart, selEnd) = sel
          editor.text.update(text => {
            def updateCursor(pos: Int, op: Operation): Int =
              op match
                case Insert(ipos, s) => if pos <= ipos then pos else pos + s.length
                case Delete(dpos, s) => if pos <= dpos then pos else pos - (math.min(pos, dpos + s.length) - dpos)
            val newText = ops.foldLeft(text)((t, op) => {
              selStart = updateCursor(selStart, op)
              selEnd = updateCursor(selEnd, op)
              applyOperation(op, t)
            })
            updateText(newText)
            newText
          })
          (selStart, selEnd)
        })
      catch
        case e: IndexOutOfBoundsException => {
          println(s"Index exception ${e.toString()}")
          try
            wsClient.disconnect()
          finally
            initWebSocket()
        }
    }

    wsClient.setOnConnectionChange(status => {
      if !status then
        initWebSocket()
    })
  }

  private def applyOperation(op: Operation, doc: String): String = {
    println(s"!!!${doc.length} $doc")
    op match {
      case Insert(index, str) =>
        if (0 <= index && index <= doc.length) {
          doc.take(index) + str + doc.drop(index)
        } else {
          throw IndexOutOfBoundsException(
            s"Insertion into an invalid position $index"
          )
        }
      case Delete(index, s) =>
        if (0 <= index && index < doc.length) {
          doc.take(index) + doc.drop(index + s.length)
        } else {
          throw IndexOutOfBoundsException(
            s"Deletion from an invalid position $index"
          )
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
      else if opType == -1 then ops = ops.appended(Delete(pos, text))

      (pos + text.length, ops)
    }
  }._2

  def onInput(e: InputEvent): Unit =
    val newText = e.target.asInstanceOf[html.TextArea].value
    handleUserAction(lastText, newText)
    updateText(newText)

  def onUndo(e: Event): Unit =
    val op = OperationHistory.revertOp
    val newText = applyOperation(op, lastText)
    wsClient.sendLocalUndo(List(op))
    updateText(newText)

  def onRedo(e: Event): Unit =
    val op = OperationHistory.revertUndoOp
    val newText = applyOperation(op, lastText)
    wsClient.sendLocalOperations(List(op))
    updateText(newText)

  def main(args: Array[String]): Unit = {
    initWebSocket()
    val div = dom.document.getElementById("app")
    val appContent = 
      <div class="app-container">
        {Logo.component}
        {editor.component}
      </div>

    mount(div, appContent)
    mount(div, undoButton.component)
    mount(div, redoButton.component)
  }
}
