package dev.mangolabs.quilleditor

import android.webkit.JavascriptInterface
import dev.mangolabs.quilleditor.model.ActiveFormat
import dev.mangolabs.quilleditor.model.Delta
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bridge between the Quill WebView and Kotlin.
 *
 * `@JavascriptInterface` methods are invoked from the WebView's JS thread. They
 * marshal onto the main thread via [onMainThread] (typically a `Handler.post`)
 * before invoking the supplied callbacks. The [dispose] flag short-circuits any
 * in-flight callback so a torn-down editor never mutates downstream state.
 */
internal class QuillBridge(
  private val onContent: (Delta) -> Unit,
  private val onFormat: (ActiveFormat) -> Unit,
  private val onReadyCallback: () -> Unit,
  private val onMainThread: (() -> Unit) -> Unit
) {
  private val disposed = AtomicBoolean(false)

  private val json = Json { ignoreUnknownKeys = true }

  fun dispose() { disposed.set(true) }

  @JavascriptInterface
  fun onContentChanged(deltaJson: String) {
    if (disposed.get()) return
    val delta = json.decodeFromString<Delta>(deltaJson)
    onMainThread { if (!disposed.get()) onContent(delta) }
  }

  @JavascriptInterface
  fun onFormatChanged(formatJson: String) {
    if (disposed.get()) return
    val format = ActiveFormat.fromJson(formatJson)
    onMainThread { if (!disposed.get()) onFormat(format) }
  }

  @JavascriptInterface
  fun onReady() {
    if (disposed.get()) return
    onMainThread { if (!disposed.get()) onReadyCallback() }
  }
}
