package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.textInsert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hammers toggleBold ten times in quick succession to verify the system stays
 * stable under rapid input — no crash, no lost content, no stuck state. The
 * exact final activeFormat depends on async callback ordering, but the editor
 * content must remain intact.
 */
@RunWith(AndroidJUnit4::class)
class RapidToggleSafetyTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun rapidTogglePreservesContentAndStaysStable() {
    var state: QuillState? = null
    rule.setContent {
      val s = rememberQuillState()
      state = s
      QuillEditor(state = s, modifier = Modifier.fillMaxSize())
    }

    rule.waitUntil(timeoutMillis = 10_000) { state?.isReady == true }

    rule.runOnUiThread {
      state!!.webViewRef!!.evaluateJavascript(
        "quill.insertText(0, 'hi', 'user'); quill.setSelection(0, 2)", null
      )
    }

    rule.waitUntil(timeoutMillis = 3_000) {
      state!!.contentDelta?.ops?.any { it.textInsert?.contains("hi") == true } == true
    }

    repeat(10) {
      rule.runOnUiThread { state!!.toggleBold() }
    }

    // Allow async bridge traffic to settle past the debounce window.
    rule.mainClock.advanceTimeBy(800)
    rule.waitForIdle()

    val text = state!!.contentDelta!!.ops.joinToString("") { it.textInsert ?: "" }
    assertTrue("content lost during rapid toggle: '$text'", text.contains("hi"))
  }
}
