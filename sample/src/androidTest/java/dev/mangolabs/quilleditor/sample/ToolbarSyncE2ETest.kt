package dev.mangolabs.quilleditor.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.QuillEditor
import dev.mangolabs.quilleditor.QuillState
import dev.mangolabs.quilleditor.QuillToolbar
import dev.mangolabs.quilleditor.model.Delta
import dev.mangolabs.quilleditor.model.DeltaOp
import dev.mangolabs.quilleditor.rememberQuillState
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end: load a bold-formatted Delta, place the cursor inside it via the
 * public setSelection API, and assert the toolbar's Bold button reflects the
 * active format. Exercises QuillEditor + QuillToolbar wired together exactly
 * as the consuming app would, using only the library's public API.
 */
@RunWith(AndroidJUnit4::class)
class ToolbarSyncE2ETest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun cursorInBoldRangeShowsBoldButtonChecked() {
    val boldText = Delta(
      listOf(
        DeltaOp(
          insert = JsonPrimitive("bold"),
          attributes = mapOf("bold" to JsonPrimitive(true))
        )
      )
    )

    var captured: QuillState? = null
    rule.setContent {
      val state = rememberQuillState()
      captured = state
      Column(modifier = Modifier.fillMaxSize()) {
        QuillEditor(
          state = state,
          modifier = Modifier.fillMaxWidth().weight(1f)
        )
        QuillToolbar(state = state, modifier = Modifier.fillMaxWidth())
      }
    }

    rule.waitUntil(timeoutMillis = 10_000) { captured?.isReady == true }

    rule.runOnUiThread { captured!!.setContent(boldText) }
    rule.runOnUiThread { captured!!.setSelection(2, 0) }

    rule.waitUntil(timeoutMillis = 3_000) { captured!!.activeFormat.bold }

    rule.onNodeWithContentDescription("Bold").assertIsOn()
  }
}
