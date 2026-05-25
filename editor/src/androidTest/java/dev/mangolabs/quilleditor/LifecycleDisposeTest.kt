package dev.mangolabs.quilleditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Triggers an in-flight JS callback and removes the editor from composition
 * in the same UI-thread block. The bridge's dispose flag must short-circuit
 * any queued callback so no state mutation reaches a torn-down composable.
 */
@RunWith(AndroidJUnit4::class)
class LifecycleDisposeTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun disposingComposableMidCallbackDoesNotCrash() {
    var captured: QuillState? = null
    var showState: ((Boolean) -> Unit)? = null

    rule.setContent {
      var show by remember { mutableStateOf(true) }
      showState = { show = it }
      if (show) {
        val s = rememberQuillState()
        captured = s
        QuillEditor(state = s, modifier = Modifier.fillMaxSize())
      }
    }

    rule.waitUntil(timeoutMillis = 10_000) { captured?.isReady == true }

    rule.runOnUiThread {
      // Queue a non-silent JS edit, then immediately dispose the composable.
      // The bridge's @JavascriptInterface callback may already be in flight
      // on the JS thread when dispose() runs.
      captured!!.webViewRef!!.evaluateJavascript(
        "quill.insertText(0, 'race', 'user')", null
      )
      showState!!.invoke(false)
    }

    rule.waitForIdle()

    // After disposal, webViewRef is cleared and no further callbacks should fire.
    assertNull(captured!!.webViewRef)
  }
}
