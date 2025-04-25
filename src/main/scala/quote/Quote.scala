package quote

import mhtml._
import scala.xml.Node
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation._

object Main {

  val count: Var[Int] = Var(0)

  val doge: Node =
      <img style="width: 100px;" src="/vite.svg"/>

  val rxDoges: Rx[Seq[Node]] =
    count.map(i => Seq.fill(i)(doge))

  val component = // ← look, you can even use fancy names!
    <div class="p-4 bg-sky-100 space-y-4">
      <button onclick={ () => count.update(_ + 1) }>Click Me!</button>
      <div class="flex space-x-4 flex-wrap">
        { count.map(i => if (i <= 0) <div></div> else <h2 class="text-gray-700">WOW!!!</h2>) }
        { count.map(i => if (i <= 2) <div></div> else <h2 class="text-gray-700 font-bold">MUCH REACTIVE!!!</h2>) }
        { count.map(i => if (i <= 5) <div></div> else <h2 class="text-gray-700 font-black">SUCH BINDING!!!</h2>) }
      </div>
      <div class="grid grid-cols-5 gap-2">
      { rxDoges }
      </div>
    </div>

  val editor =
    <textarea class="mainTextArea"></textarea>

  def main(args: Array[String]): Unit = {
    val div = dom.document.getElementById("app")
    mount(div, editor)
  }

}
