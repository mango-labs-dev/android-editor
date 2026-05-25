package dev.mangolabs.quilleditor

import dev.mangolabs.quilleditor.model.ActiveFormat
import dev.mangolabs.quilleditor.model.Delta
import dev.mangolabs.quilleditor.model.textInsert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuillBridgeTest {

  private val synchronousMain: (() -> Unit) -> Unit = { block -> block() }

  @Test
  fun onReadyFiresCallback() {
    var ready = false
    val bridge = QuillBridge(
      onContent = { },
      onFormat = { },
      onReadyCallback = { ready = true },
      onMainThread = synchronousMain
    )
    bridge.onReady()
    assertTrue(ready)
  }

  @Test
  fun onContentChangedParsesDeltaAndInvokesCallback() {
    var received: Delta? = null
    val bridge = QuillBridge(
      onContent = { received = it },
      onFormat = { },
      onReadyCallback = { },
      onMainThread = synchronousMain
    )
    bridge.onContentChanged("""{"ops":[{"insert":"hi"}]}""")
    assertEquals(1, received?.ops?.size)
    assertEquals("hi", received?.ops?.get(0)?.textInsert)
  }

  @Test
  fun onFormatChangedParsesActiveFormatAndInvokesCallback() {
    var received: ActiveFormat? = null
    val bridge = QuillBridge(
      onContent = { },
      onFormat = { received = it },
      onReadyCallback = { },
      onMainThread = synchronousMain
    )
    bridge.onFormatChanged("""{"bold":true,"list":"bullet"}""")
    assertEquals(true, received?.bold)
    assertEquals("bullet", received?.list)
  }

  @Test
  fun onFormatChangedWithEmptyObjectResetsToDefaults() {
    var received: ActiveFormat? = null
    val bridge = QuillBridge(
      onContent = { },
      onFormat = { received = it },
      onReadyCallback = { },
      onMainThread = synchronousMain
    )
    bridge.onFormatChanged("{}")
    assertEquals(ActiveFormat(), received)
  }

  @Test
  fun disposedBridgeIgnoresSubsequentCallbacks() {
    var contentCalls = 0
    var formatCalls = 0
    var readyCalls = 0
    val bridge = QuillBridge(
      onContent = { contentCalls++ },
      onFormat = { formatCalls++ },
      onReadyCallback = { readyCalls++ },
      onMainThread = synchronousMain
    )
    bridge.dispose()
    bridge.onContentChanged("""{"ops":[]}""")
    bridge.onFormatChanged("{}")
    bridge.onReady()
    assertEquals(0, contentCalls)
    assertEquals(0, formatCalls)
    assertEquals(0, readyCalls)
  }

  @Test
  fun disposeBetweenMarshalAndExecutionPreventsCallback() {
    // Simulates the race: JS thread calls bridge → bridge queues onMainThread block →
    // WebView is destroyed and bridge disposed → main thread later runs queued block.
    // The block must be a no-op because the bridge is disposed.
    var contentCalls = 0
    val pending = mutableListOf<() -> Unit>()
    val deferredMain: (() -> Unit) -> Unit = { block -> pending.add(block) }
    val bridge = QuillBridge(
      onContent = { contentCalls++ },
      onFormat = { },
      onReadyCallback = { },
      onMainThread = deferredMain
    )
    bridge.onContentChanged("""{"ops":[{"insert":"x"}]}""")
    bridge.dispose()
    pending.forEach { it() }
    assertEquals(0, contentCalls)
  }
}
