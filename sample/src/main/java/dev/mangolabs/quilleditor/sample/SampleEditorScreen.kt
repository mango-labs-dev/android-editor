package dev.mangolabs.quilleditor.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.mangolabs.quilleditor.QuillEditor
import dev.mangolabs.quilleditor.QuillToolbar
import dev.mangolabs.quilleditor.rememberQuillState

/**
 * Phase 4 — sample now hosts QuillEditor and QuillToolbar from :editor. The
 * editor takes the remaining vertical space; the toolbar pins to the bottom
 * and reacts to cursor formatting via the shared QuillState.
 */
@Composable
fun SampleEditorScreen(modifier: Modifier = Modifier) {
  val state = rememberQuillState()
  Column(modifier = modifier.fillMaxSize()) {
    QuillEditor(
      state = state,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    )
    HorizontalDivider()
    QuillToolbar(state = state, modifier = Modifier.fillMaxWidth())
  }
}
