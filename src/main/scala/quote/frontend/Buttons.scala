package quote.frontend

import org.scalajs.dom.*
import mhtml.*

import quote.ot.*

class UndoButton(onClick: Event => Unit) {
  def component = <button id="undo" class="doButton" onclick={onClick}>undO</button>
}

class RedoButton(onClick: Event => Unit) {
  def component = <button id="redo" class="doButton" onclick={onClick}>redO</button>
}
