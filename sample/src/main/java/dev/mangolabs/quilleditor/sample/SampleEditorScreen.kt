package dev.mangolabs.quilleditor.sample

import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "QuillSample"
private const val EDITOR_URL = "file:///android_asset/quill/editor.html"

/**
 * Phase 1 smoke screen — hosts the bundled editor.html in a bare WebView.
 *
 * No QuillState, no toolbar, no Kotlin↔JS bridge yet. The page is expected to
 * load Quill, instantiate an empty editor, and stay error-free. Phase 3 will
 * replace this with the QuillEditor composable from :editor.
 */
@Composable
fun SampleEditorScreen(modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier.fillMaxSize(),
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
            Log.d(TAG, "[JS ${message.messageLevel()}] ${message.message()} @${message.lineNumber()}")
            return true
          }
        }

        loadUrl(EDITOR_URL)
      }
    }
  )
}
