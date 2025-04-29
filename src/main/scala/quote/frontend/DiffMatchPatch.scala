package quote.frontend

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("diff_match_patch")
class DiffMatchPatch extends js.Object {
  def diff_main(old: String, new_ : String): js.Array[js.Array[Any]] = js.native
  def diff_cleanupSemantic(diffs: js.Array[js.Array[Any]]): Unit = js.native
}
