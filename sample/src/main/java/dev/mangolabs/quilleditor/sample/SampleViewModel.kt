package dev.mangolabs.quilleditor.sample

import androidx.lifecycle.ViewModel
import dev.mangolabs.quilleditor.model.Delta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the editor's saved Delta across configuration changes (rotation,
 * dark-mode flips, font-scale changes — anything that recreates the
 * Activity). The sample autosaves [SampleEditorScreen]'s contentDelta into
 * this VM, then loads it back on remount.
 *
 * Real apps would back this with Room and a coroutine-scoped repository;
 * the in-memory StateFlow here is just enough to demonstrate the pattern.
 */
class SampleViewModel : ViewModel() {

  private val _savedDelta = MutableStateFlow<Delta?>(null)
  val savedDelta: StateFlow<Delta?> = _savedDelta.asStateFlow()

  fun setSavedDelta(delta: Delta?) {
    _savedDelta.value = delta
  }
}
