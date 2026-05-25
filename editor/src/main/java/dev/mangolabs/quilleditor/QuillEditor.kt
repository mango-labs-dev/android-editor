package dev.mangolabs.quilleditor

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

private const val TAG = "QuillEditor"
private const val EDITOR_URL = "file:///android_asset/quill/editor.html"
private const val BRIDGE_NAME = "KotlinBridge"

@Composable
fun QuillEditor(
  state: QuillState,
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = isSystemInDarkTheme()
) {
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  val initialDelta = remember { state.contentDelta }
  val bridgeHolder = remember { arrayOfNulls<QuillBridge>(1) }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      WebView(ctx).apply {
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          allowFileAccess = false
          allowContentAccess = false
          mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        setBackgroundColor(Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        webChromeClient = object : WebChromeClient() {
          override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            Log.d(
              TAG,
              "[JS ${message.messageLevel()}] ${message.message()} @${message.lineNumber()}"
            )
            return true
          }
        }

        val bridge = QuillBridge(
          onContent = { delta -> state.contentDelta = delta },
          onFormat = { format -> state.activeFormat = format },
          onReadyCallback = {
            state.isReady = true
            evaluateJavascript("window.setDarkMode($isDarkTheme)", null)
            initialDelta?.let {
              val payload = Json.encodeToString(it)
              evaluateJavascript(
                "window.setContents(${JSONObject.quote(payload)})", null
              )
            }
          },
          onMainThread = { block -> mainHandler.post(block) }
        )
        bridgeHolder[0] = bridge
        addJavascriptInterface(bridge, BRIDGE_NAME)

        loadUrl(EDITOR_URL)
        state.webViewRef = this
      }
    },
    onRelease = { webView ->
      bridgeHolder[0]?.dispose()
      state.webViewRef = null
      webView.removeJavascriptInterface(BRIDGE_NAME)
      webView.destroy()
    }
  )

  LaunchedEffect(isDarkTheme, state.isReady) {
    if (state.isReady) {
      state.webViewRef?.evaluateJavascript("window.setDarkMode($isDarkTheme)", null)
    }
  }
}
