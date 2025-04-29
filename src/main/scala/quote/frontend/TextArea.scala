package quote.frontend

import org.scalajs.dom.*
import mhtml.*
import scala.collection.immutable.Queue

import quote.ot.*

object TextArea {
  private var lastText: String = ""
  private val dmp = DiffMatchPatch()

  private def onInput(e: InputEvent): Unit =
    val newText = e.target.asInstanceOf[html.TextArea].value
    val diffs = dmp.diff_main(lastText, newText)
    dmp.diff_cleanupSemantic(diffs)

    diffs.foldLeft(0) { (pos, diff) =>
      val (opType, text) = (diff(0).asInstanceOf[Int], diff(1).asInstanceOf[String])
      if opType == 1 then
        OperationHistory.putInsertOp(Insert(pos, text))
      else if opType == -1 then
        OperationHistory.putDeleteOp(Delete(pos, text.length), text)
      pos + text.length
    }
    
    lastText = newText

  def getMainComponent() =
    val textarea = <textarea class="mainTextArea" oninput={onInput}></textarea>
    textarea
}
