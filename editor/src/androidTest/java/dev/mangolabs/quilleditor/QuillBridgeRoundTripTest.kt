package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.textInsert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mounts the editor, simulates a typed insertion via Quill's `insertText`
 * call (which fires the non-silent `text-change` event), and asserts that
 * the Kotlin-side `QuillState.contentDelta` mirror updates within the
 * debounced window (~200ms + slack).
 */
@RunWith(AndroidJUnit4::class)
class QuillBridgeRoundTripTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun insertTextPropagatesToContentDelta() {
    var captured: QuillState? = null
    rule.setContent {
      val s = rememberQuillState()
      captured = s
      QuillEditor(state = s, modifier = Modifier.fillMaxSize())
    }

    rule.waitUntil(timeoutMillis = 10_000) { captured?.isReady == true }

    rule.runOnUiThread {
      captured!!.webViewRef!!.evaluateJavascript(
        "quill.insertText(0, 'Hi there', 'user')", null
      )
    }

    rule.waitUntil(timeoutMillis = 3_000) {
      captured?.contentDelta?.ops?.any { it.textInsert?.contains("Hi there") == true } == true
    }

    val ops = captured!!.contentDelta!!.ops
    assertNotNull(ops)
    assertTrue(
      "expected at least one op containing 'Hi there', got $ops",
      ops.any { it.textInsert?.contains("Hi there") == true }
    )
  }
}
