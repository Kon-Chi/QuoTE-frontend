package quote.frontend

import org.scalajs.dom.*
import mhtml.Var

import quote.ot.*

class TextArea(initalText: String, onInput: InputEvent => Unit) {
  val text: Var[String] = Var(initalText)

  def component = <textarea class="mainTextArea" oninput={onInput}>{text}</textarea>
}
