package dev.mangolabs.quilleditor.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.mangolabs.quilleditor.QuillEditor
import dev.mangolabs.quilleditor.rememberQuillState

/**
 * Phase 3 — consumes the QuillEditor composable from :editor instead of a bare
 * WebView. QuillState is hoisted here so future phases can hook autosave,
 * format-toolbar wiring, image insertion, etc. without touching the library.
 */
@Composable
fun SampleEditorScreen(modifier: Modifier = Modifier) {
  val state = rememberQuillState()
  QuillEditor(state = state, modifier = modifier.fillMaxSize())
}
