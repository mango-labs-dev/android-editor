package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Toggling bold via QuillState updates the cursor format reported by Quill,
 * which Quill in turn pushes back to Kotlin via the selection-change event
 * (immediate, not debounced). After [QuillState.toggleBold] the activeFormat
 * mirror should report bold within a short window.
 */
@RunWith(AndroidJUnit4::class)
class FormatSyncTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun toggleBoldUpdatesActiveFormat() {
    var state: QuillState? = null
    rule.setContent {
      val s = rememberQuillState()
      state = s
      QuillEditor(state = s, modifier = Modifier.fillMaxSize())
    }

    rule.waitUntil(timeoutMillis = 10_000) { state?.isReady == true }

    rule.runOnUiThread {
      state!!.webViewRef!!.evaluateJavascript(
        "quill.insertText(0, 'hello', 'user'); quill.setSelection(0, 5)", null
      )
    }

    rule.waitUntil(timeoutMillis = 3_000) { state!!.contentDelta != null }

    rule.runOnUiThread { state!!.toggleBold() }

    rule.waitUntil(timeoutMillis = 3_000) { state!!.activeFormat.bold }
    assertTrue(state!!.activeFormat.bold)
  }
}
