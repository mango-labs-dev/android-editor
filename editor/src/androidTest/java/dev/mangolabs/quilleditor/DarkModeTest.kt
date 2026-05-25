package dev.mangolabs.quilleditor

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Flipping the `isDarkTheme` flag on [QuillEditor] adds/removes the
 * `dark` class on `<body>` via the LaunchedEffect-driven setDarkMode call.
 * The CSS variables under `body.dark` then re-skin the editor.
 */
@RunWith(AndroidJUnit4::class)
class DarkModeTest {

  @get:Rule
  val rule = createComposeRule()

  @Test
  fun darkModeFlagPropagatesToBodyClass() {
    val isDark = mutableStateOf(false)
    var state: QuillState? = null
    rule.setContent {
      val s = rememberQuillState()
      state = s
      QuillEditor(
        state = s,
        modifier = Modifier.fillMaxSize(),
        isDarkTheme = isDark.value
      )
    }

    rule.waitUntil(timeoutMillis = 10_000) { state?.isReady == true }

    assertFalse(
      "body should not have 'dark' class while isDarkTheme = false",
      queryBodyHasDarkClass(state!!.webViewRef!!)
    )

    rule.runOnUiThread { isDark.value = true }
    rule.waitForIdle()
    assertTrue(
      "body should gain 'dark' class after toggling isDarkTheme = true",
      pollUntil(3_000) { queryBodyHasDarkClass(state!!.webViewRef!!) }
    )

    rule.runOnUiThread { isDark.value = false }
    rule.waitForIdle()
    assertTrue(
      "body should lose 'dark' class after toggling isDarkTheme = false",
      pollUntil(3_000) { !queryBodyHasDarkClass(state!!.webViewRef!!) }
    )
  }

  private fun pollUntil(deadlineMs: Long, predicate: () -> Boolean): Boolean {
    val end = System.currentTimeMillis() + deadlineMs
    while (System.currentTimeMillis() < end) {
      if (predicate()) return true
      Thread.sleep(100)
    }
    return false
  }

  private fun queryBodyHasDarkClass(webView: WebView): Boolean {
    val latch = CountDownLatch(1)
    var result = false
    rule.runOnUiThread {
      webView.evaluateJavascript("document.body.classList.contains('dark')") { value ->
        result = value?.trim() == "true"
        latch.countDown()
      }
    }
    latch.await(500, TimeUnit.MILLISECONDS)
    return result
  }
}
