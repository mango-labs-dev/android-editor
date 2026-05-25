package dev.mangolabs.quilleditor

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream

/**
 * Intercepts requests with the synthetic `app-image://` scheme and serves
 * the bytes from `filesDir/images/<host>.jpg`. Returning `null` falls back
 * to the WebView's default request handling — the missing-file case yields
 * a broken `<img>` tag, but the WebView does not crash.
 *
 * The FileInputStream is closed by WebResourceResponse once the WebView
 * finishes consuming the body. Do not close it here.
 */
internal class QuillWebViewClient(private val filesDir: File) : WebViewClient() {

  override fun shouldInterceptRequest(
    view: WebView,
    request: WebResourceRequest
  ): WebResourceResponse? {
    val uri = request.url
    if (uri.scheme != APP_IMAGE_SCHEME) return null

    val id = uri.host ?: return null
    val file = File(filesDir, "images/$id.jpg")
    if (!file.exists()) return null

    return WebResourceResponse(
      "image/jpeg",
      null,
      FileInputStream(file)
    )
  }

  companion object {
    const val APP_IMAGE_SCHEME = "app-image"
  }
}
