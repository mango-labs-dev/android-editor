## Known issues — quill-compose-editor

Outstanding issues observed in consumer apps. Each item is a candidate for a
patch release once addressed.

### 1. Inline styles dropped when applying bullet / numbered list

**Observed:** With bold (or any inline format) active, applying a bullet or
ordered-list block format drops the inline style on the typed text — new
characters land unstyled.

**Expected:** Inline formats remain active across block-format changes. Quill
itself keeps active formats on; this likely originates in the toolbar's
toggle wiring or in how `state.activeFormat` is reconciled after a block
format is applied. See `QuillToolbar` + `QuillBridge.onFormat` interaction.

**Reproduce:** Tap **B**, type "abc", tap bullet-list, type "def". "abc"
should be bold; "def" should also be bold (and a list item) — currently
"def" is plain.

### 2. Tab spacing is visually too wide

**Observed:** Pressing Tab (or an indent action that maps to `Tab.set`) inserts
~4 character widths of horizontal space, which dominates short lines on
mobile.

**Expected:** A tighter default — 2 spaces' visual width, or use the standard
list-indent convention (1.5em / 2em) rather than `tab-size: 4`.

**Where:** `editor/src/main/assets/quill/quill.core.css` ships `.ql-editor
{ tab-size: 4 }`. Either override in `editor.css` (`tab-size: 2`) or stop
emitting tab characters in favour of Quill's indent formats.

### 3. Default editor font size is too small on mobile

**Observed:** Typed text renders at ~13px (Quill's core default for
`.ql-container { font-size: 13px }`), which is below platform readable
size on a phone.

**Expected:** Body text should match the Android default for content (~16sp),
i.e. roughly `font-size: 16px` in the WebView.

**Where:** `editor.css` sets `html, body { font-size: 16px }`, but the
`.ql-container` rule from `quill.core.css` overrides it for the actual editor
text. Add `font-size: 16px` (or inherit) to `.ql-container` in `editor.css`.
