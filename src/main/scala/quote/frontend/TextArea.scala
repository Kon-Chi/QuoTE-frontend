package quote.frontend

import org.scalajs.dom.*
import mhtml.*

import quote.ot.*

class TextArea(initalText: String, onInput: InputEvent => Unit) {
  val text: Var[String] = Var(initalText)
  val selection: Var[(Int, Int)] = Var((initalText.length, initalText.length))

  // Cursed workaround for updating .value and .selectionStart
  private val updText: Rx[String] = text.map(s => {
    val e = document.getElementById("editor")
    if e != null then
      println("aaa")
      e.asInstanceOf[html.TextArea].value = s
    ""
  })
  private val updCursor: Rx[String] = selection.map(i => {
    val e = document.getElementById("editor")
    if e != null then
      println("ccc")
      e.asInstanceOf[html.TextArea].selectionStart = i._1
      e.asInstanceOf[html.TextArea].selectionEnd = i._2
    ""
  })

  private def onCursorMove(e: Event) =
    val e = document.getElementById("editor")
    if e != null then
      val ta = e.asInstanceOf[html.TextArea]
      selection := (ta.selectionStart, ta.selectionEnd)

  def component = <textarea id="editor" stub={updText} stub2={updCursor} class="mainTextArea" oninput={onInput} onselectionchange={onCursorMove}></textarea>
}
