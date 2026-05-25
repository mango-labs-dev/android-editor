package dev.mangolabs.quilleditor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.imageUrl
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Writes a real JPEG into filesDir/images/<uuid>.jpg, inserts an
 * app-image://<uuid> embed, and asserts both that the Delta records the
 * embed and that the WebView actually loaded the image (naturalWidth > 0,
 * which is only true if QuillWebViewClient intercepted the request and
 * served the bytes from disk).
 */
@RunWith(AndroidJUnit4::class)
class ImageSchemeTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun appImageSchemeServesFileAndImageLoads() {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    val imagesDir = File(ctx.filesDir, "images").apply { mkdirs() }
    val id = "scheme-test-${System.nanoTime()}"
    val testFile = File(imagesDir, "$id.jpg")
    testFile.writeBytes(makeJpeg(32, 32, Color.RED))

    try {
      var state: QuillState? = null
      rule.setContent {
        val s = rememberQuillState()
        state = s
        QuillEditor(state = s, modifier = Modifier.fillMaxSize())
      }
      rule.waitUntil(timeoutMillis = 10_000) { state?.isReady == true }

      rule.runOnUiThread { state!!.insertImage("app-image://$id") }

      rule.waitUntil(timeoutMillis = 3_000) {
        state?.contentDelta?.ops?.any { it.imageUrl == "app-image://$id" } == true
      }

      // Poll naturalWidth via JS — only > 0 if the WebView successfully fetched
      // the image bytes through QuillWebViewClient.shouldInterceptRequest.
      val loaded = pollNaturalWidth(state!!.webViewRef!!, deadlineMs = 5_000)
      assertTrue("image did not load via app-image scheme", loaded)
    } finally {
      testFile.delete()
    }
  }

  private fun pollNaturalWidth(webView: android.webkit.WebView, deadlineMs: Long): Boolean {
    val end = System.currentTimeMillis() + deadlineMs
    while (System.currentTimeMillis() < end) {
      val latch = CountDownLatch(1)
      var width = 0
      rule.runOnUiThread {
        webView.evaluateJavascript(
          "(function(){var i=document.querySelector('img');return i?i.naturalWidth:0;})()"
        ) { result ->
          width = result?.trim('"')?.toIntOrNull() ?: 0
          latch.countDown()
        }
      }
      latch.await(500, TimeUnit.MILLISECONDS)
      if (width > 0) return true
      Thread.sleep(100)
    }
    return false
  }

  private fun makeJpeg(width: Int, height: Int, color: Int): ByteArray {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(color)
    return ByteArrayOutputStream().use {
      bmp.compress(Bitmap.CompressFormat.JPEG, 80, it)
      it.toByteArray()
    }
  }
}
