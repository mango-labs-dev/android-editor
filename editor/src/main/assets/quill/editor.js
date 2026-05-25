// Quill v2.0.3 — do not bump without re-running the full instrumented test suite.
// KotlinBridge is injected by QuillEditor.kt at runtime. Calls are guarded so
// the page can also be opened in a plain browser (smoke test, debugging).

let quill;

function bridge() {
  return typeof KotlinBridge !== 'undefined' ? KotlinBridge : null;
}

document.addEventListener('DOMContentLoaded', () => {
  quill = new Quill('#editor', {
    modules: {
      toolbar: false,
      history: { delay: 500, maxStack: 100 }
    },
    placeholder: 'Start writing...',
    formats: ['bold', 'italic', 'underline', 'strike', 'list', 'header', 'link', 'image']
  });

  // Push content changes to Kotlin (debounced).
  let debounceTimer;
  quill.on('text-change', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const b = bridge();
      if (!b) return;
      const delta = JSON.stringify(quill.getContents());
      b.onContentChanged(delta);
    }, 200);
  });

  // Push selection/format state to Kotlin (immediate, for toolbar sync).
  quill.on('selection-change', (range) => {
    const b = bridge();
    if (!b) return;
    if (range) {
      const format = quill.getFormat(range);
      b.onFormatChanged(JSON.stringify(format));
    } else {
      // Reset toolbar on blur / deselect so it does not show stale format.
      b.onFormatChanged('{}');
    }
  });

  const b = bridge();
  if (b) b.onReady();
});

// === Functions callable from Kotlin ===

window.setContents = (deltaJson) => {
  quill.setContents(JSON.parse(deltaJson), 'silent');
};

window.applyFormat = (formatName, value) => {
  const range = quill.getSelection(true);
  if (!range) return;
  if (range.length === 0) {
    quill.format(formatName, value);
  } else {
    quill.formatText(range.index, range.length, formatName, value);
  }
};

window.insertImage = (src) => {
  const range = quill.getSelection(true) || { index: quill.getLength() };
  quill.insertEmbed(range.index, 'image', src, 'user');
  quill.setSelection(range.index + 1);
};

window.undo = () => quill.history.undo();
window.redo = () => quill.history.redo();
window.setDarkMode = (isDark) => document.body.classList.toggle('dark', isDark);
window.focusEditor = () => quill.focus();
