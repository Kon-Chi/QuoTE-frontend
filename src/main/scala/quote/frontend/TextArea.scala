package quote.frontend

import org.scalajs.dom.*
import mhtml.*

import quote.ot.*

class TextArea(initalText: String, onInput: InputEvent => Unit) {
  val text: Var[String] = Var(initalText)
  val upd: Rx[String] = text.map(s => {
    val e = document.getElementById("editor")
    if e != null then
      println("aaa")
      e.asInstanceOf[html.TextArea].value = s
    ""
  })

  def component = <textarea id="editor" stub={upd} class="mainTextArea" oninput={onInput}></textarea>
}

