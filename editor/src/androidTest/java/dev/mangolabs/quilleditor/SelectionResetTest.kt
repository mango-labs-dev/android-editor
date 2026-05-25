package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.ActiveFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * When the editor loses selection (blur), Quill's selection-change fires with
 * a null range. editor.js maps that to KotlinBridge.onFormatChanged('{}'),
 * which resets activeFormat to defaults — preventing toolbar stale state.
 */
@RunWith(AndroidJUnit4::class)
class SelectionResetTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun blurResetsActiveFormat() {
    var state: QuillState? = null
    rule.setContent {
      val s = rememberQuillState()
      state = s
      QuillEditor(state = s, modifier = Modifier.fillMaxSize())
    }

    rule.waitUntil(timeoutMillis = 10_000) { state?.isReady == true }

    rule.runOnUiThread {
      state!!.webViewRef!!.evaluateJavascript(
        "quill.insertText(0, 'bold', { bold: true }, 'user'); quill.setSelection(0, 4)", null
      )
    }

    rule.waitUntil(timeoutMillis = 3_000) { state!!.activeFormat.bold }

    rule.runOnUiThread {
      state!!.webViewRef!!.evaluateJavascript("quill.blur()", null)
    }

    rule.waitUntil(timeoutMillis = 3_000) { state!!.activeFormat == ActiveFormat() }
    assertEquals(ActiveFormat(), state!!.activeFormat)
  }
}
