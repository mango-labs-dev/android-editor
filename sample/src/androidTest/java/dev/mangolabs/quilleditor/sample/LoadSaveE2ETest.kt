package dev.mangolabs.quilleditor.sample

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.QuillState
import dev.mangolabs.quilleditor.model.Delta
import dev.mangolabs.quilleditor.model.DeltaOp
import dev.mangolabs.quilleditor.model.textInsert
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The autosave pattern: SampleEditorScreen mirrors QuillState.contentDelta
 * into SampleViewModel as it changes, and on a fresh mount it pulls the
 * saved Delta back through rememberQuillState's initial parameter. This
 * test verifies that round-trip across an unmount/remount cycle of the
 * same composable instance.
 */
@RunWith(AndroidJUnit4::class)
class LoadSaveE2ETest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun savedDeltaReloadsAfterRemount() {
    val vm = SampleViewModel()
    val saved = Delta(
      listOf(DeltaOp(insert = JsonPrimitive("persistent content")))
    )
    vm.setSavedDelta(saved)

    val show = mutableStateOf(true)
    val captured = arrayOfNulls<QuillState>(1)

    rule.setContent {
      if (show.value) {
        SampleEditorScreen(vm = vm, onStateReady = { captured[0] = it })
      }
    }

    rule.waitUntil(timeoutMillis = 10_000) { captured[0]?.isReady == true }
    rule.waitUntil(timeoutMillis = 3_000) {
      captured[0]?.contentDelta?.ops?.any {
        it.textInsert?.contains("persistent content") == true
      } == true
    }

    captured[0] = null
    rule.runOnUiThread { show.value = false }
    rule.waitForIdle()

    rule.runOnUiThread { show.value = true }

    rule.waitUntil(timeoutMillis = 10_000) { captured[0]?.isReady == true }
    rule.waitUntil(timeoutMillis = 3_000) {
      captured[0]?.contentDelta?.ops?.any {
        it.textInsert?.contains("persistent content") == true
      } == true
    }

    val ops = captured[0]!!.contentDelta!!.ops
    assertTrue(
      "expected persistent content after remount, got $ops",
      ops.any { it.textInsert?.contains("persistent content") == true }
    )
  }
}
