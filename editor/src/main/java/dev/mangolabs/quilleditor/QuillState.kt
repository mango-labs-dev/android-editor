package dev.mangolabs.quilleditor

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mangolabs.quilleditor.model.ActiveFormat
import dev.mangolabs.quilleditor.model.Delta
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * State holder exposing the editor's content and active formatting to Compose.
 * Mutations are made by the bridge callbacks (see [QuillEditor]); calls from
 * the consuming app go the other direction via the public format/insert/undo
 * methods that evaluate JS on the underlying WebView.
 */
@Stable
class QuillState {

  var contentDelta by mutableStateOf<Delta?>(null)
    internal set

  var activeFormat by mutableStateOf(ActiveFormat())
    internal set

  var isReady by mutableStateOf(false)
    internal set

  internal var webViewRef: WebView? = null

  fun toggleBold() = applyFormat("bold", !activeFormat.bold)
  fun toggleItalic() = applyFormat("italic", !activeFormat.italic)
  fun toggleUnderline() = applyFormat("underline", !activeFormat.underline)
  fun toggleStrike() = applyFormat("strike", !activeFormat.strike)

  fun toggleBulletList() {
    val next: Any = if (activeFormat.list == "bullet") false else "bullet"
    applyFormat("list", next)
  }

  fun toggleOrderedList() {
    val next: Any = if (activeFormat.list == "ordered") false else "ordered"
    applyFormat("list", next)
  }

  fun setHeader(level: Int?) = applyFormat("header", level ?: false)

  fun insertImage(url: String) {
    evalJs("window.insertImage(${JSONObject.quote(url)})")
  }

  /**
   * Replaces editor contents. The underlying call uses Quill's `'silent'`
   * source, which suppresses `text-change`, so we mirror the new value into
   * [contentDelta] here to keep Kotlin state in sync.
   */
  fun setContent(delta: Delta) {
    val payload = Json.encodeToString(delta)
    evalJs("window.setContents(${JSONObject.quote(payload)})")
    contentDelta = delta
  }

  /**
   * Moves the cursor to [index]. A non-zero [length] creates a selection range
   * starting at [index]. Triggers Quill's selection-change event so the
   * activeFormat mirror updates synchronously with the new caret position.
   */
  fun setSelection(index: Int, length: Int = 0) {
    evalJs("window.setSelection($index, $length)")
  }

  fun undo() = evalJs("window.undo()")
  fun redo() = evalJs("window.redo()")
  fun focus() = evalJs("window.focusEditor()")

  private fun applyFormat(name: String, value: Any) {
    val jsValue = when (value) {
      is Boolean -> value.toString()
      is String -> JSONObject.quote(value)
      is Number -> value.toString()
      else -> "null"
    }
    evalJs("window.applyFormat(${JSONObject.quote(name)}, $jsValue)")
  }

  private fun evalJs(script: String) {
    val w = webViewRef ?: return
    w.post {
      // Re-read inside the post — webViewRef may have been cleared between
      // queuing this block and the main thread running it (e.g., onRelease).
      webViewRef?.evaluateJavascript(script, null)
    }
  }
}

@Composable
fun rememberQuillState(initial: Delta? = null): QuillState {
  return remember { QuillState().also { if (initial != null) it.contentDelta = initial } }
}
