package quote.frontend

import mhtml.*
import scala.xml.Node
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native @JSImport ("/public/logo.jpg", JSImport.Default)
val logo: String = js.native

object Logo {
    val component: Node = {
        <div class="logo-container">
        <img src={logo} class="logo-image" alt="Logo"/>
        <span>QuoTE: Collaborative Text Editor</span>
        </div>
    }
}