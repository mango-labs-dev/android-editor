package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.Delta
import dev.mangolabs.quilleditor.model.DeltaOp
import dev.mangolabs.quilleditor.model.textInsert
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies `setContent` mirrors into `contentDelta` synchronously (the
 * Step-7 spec fix), and that further JS-side edits accumulate on top of
 * the loaded content rather than silently being lost.
 */
@RunWith(AndroidJUnit4::class)
class SetContentRoundTripTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun setContentMirrorsAndAcceptsFurtherEdits() {
    val initial = Delta(listOf(DeltaOp(insert = JsonPrimitive("Hello"))))
    var captured: QuillState? = null

    rule.setContent {
      val s = rememberQuillState()
      captured = s
      QuillEditor(state = s, modifier = Modifier.fillMaxSize())
    }

    rule.waitUntil(timeoutMillis = 10_000) { captured?.isReady == true }

    rule.runOnUiThread { captured!!.setContent(initial) }

    // contentDelta mirror is synchronous.
    assertEquals("Hello", captured!!.contentDelta!!.ops.firstOrNull()?.textInsert)

    // Now append via JS-side insertText — fires non-silent text-change.
    rule.runOnUiThread {
      captured!!.webViewRef!!.evaluateJavascript(
        "quill.insertText(quill.getLength() - 1, ' world', 'user')", null
      )
    }

    rule.waitUntil(timeoutMillis = 3_000) {
      val joined = captured?.contentDelta?.ops?.joinToString("") { it.textInsert ?: "" } ?: ""
      joined.contains("Hello world")
    }

    val joined = captured!!.contentDelta!!.ops.joinToString("") { it.textInsert ?: "" }
    assertTrue("expected 'Hello world' in joined text, got '$joined'", joined.contains("Hello world"))
  }
}
