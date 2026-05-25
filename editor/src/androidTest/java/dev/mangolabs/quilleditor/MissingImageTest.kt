package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.imageUrl
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Inserts an app-image://<uuid> embed for a UUID with no file on disk.
 * QuillWebViewClient must return null from shouldInterceptRequest (no file
 * found) and the WebView must handle the broken image gracefully — no crash,
 * Delta still records the embed so downstream code can detect/repair it.
 */
@RunWith(AndroidJUnit4::class)
class MissingImageTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun missingImageDoesNotCrashAndStillRecordsEmbed() {
    var state: QuillState? = null
    rule.setContent {
      val s = rememberQuillState()
      state = s
      QuillEditor(state = s, modifier = Modifier.fillMaxSize())
    }

    rule.waitUntil(timeoutMillis = 10_000) { state?.isReady == true }

    val id = "missing-${System.nanoTime()}"
    rule.runOnUiThread { state!!.insertImage("app-image://$id") }

    rule.waitUntil(timeoutMillis = 3_000) {
      state?.contentDelta?.ops?.any { it.imageUrl == "app-image://$id" } == true
    }

    val embed = state!!.contentDelta!!.ops.firstOrNull { it.imageUrl == "app-image://$id" }
    assertTrue("expected app-image embed in Delta", embed != null)
  }
}
