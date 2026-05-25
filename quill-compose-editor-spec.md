# Quill-Based Compose Rich Text Editor — Implementation Spec

## Overview

Build a reusable Jetpack Compose component that wraps Quill.js (running in a `WebView`) and exposes a clean Kotlin API. The rest of the app sees only Compose state and Quill Delta JSON — it does not need to know there is a WebView underneath.

Delivered as an **Android library artifact** (`dev.mangolabs:quill-compose-editor`) with a colocated **sample application** in the same repository for in-repo verification. Consumers depend on the published AAR; developers iterate against `project(":editor")` directly so no publish step is needed during development.

This component is the foundation for a larger Android notes app (similar to Google Keep, scoped down: notes list, editor, basic formatting, image support). This spec covers **only the editor component**. The notes app integration is downstream.

### Design goals

- **Native-feel chrome:** The toolbar, screen scaffolding, and surrounding UI are 100% Compose / Material 3. Only the content area is a WebView.
- **Clean Kotlin API:** Consumers interact via a `QuillState` object and a `QuillEditor` composable. No raw `WebView` exposure.
- **Quill Delta as the storage format:** Delta JSON is what gets persisted to Room. It is well-understood, deterministic, and round-trips cleanly.
- **Offline-first:** Quill assets are bundled in `assets/`, not loaded from a CDN.
- **TDD-first:** Every behavioural unit ships with tests written before the implementation.
- **Publishable:** Library is built to publish to Maven (local first; remote registry later).

### Non-goals (for this component)

- No notes list, no Room layer, no auto-save loop — those live in the app that uses this component.
- No collaboration / OT / CRDT.
- No tables, no code blocks with syntax highlighting, no video embeds.

---

## Tech Stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Web layer | Quill.js core build, version pinned at `2.0.3` (bundled in assets) |
| Bridge | `@JavascriptInterface` + `evaluateJavascript` |
| Serialization | `kotlinx.serialization` |
| Min SDK | 34 |
| Target SDK | 36 |
| Compile SDK | 36 |
| Kotlin / JVM | Kotlin 2.x, JVM target 17, Java toolchain 17 |
| Build | Multi-module Gradle (Kotlin DSL), version catalog (`libs.versions.toml`) |
| Publishing | `maven-publish` plugin, Dokka HTML, sources JAR |
| License | Apache 2.0 |
| CI | GitHub Actions (build + unit + instrumented tests on every push) |

---

## Project Layout

```
android-editor/                          ← repo root
├── settings.gradle.kts                  ← includes :editor, :sample
├── build.gradle.kts                     ← root, no plugins applied
├── gradle/
│   └── libs.versions.toml               ← version catalog (single source)
├── .editorconfig                        ← enforces no-wildcard imports, 2-space indent
├── .github/workflows/ci.yml             ← build + test
├── LICENSE                              ← Apache 2.0
│
├── editor/                              ← LIBRARY MODULE (publishes AAR)
│   ├── build.gradle.kts                 ← com.android.library + maven-publish + dokka
│   ├── consumer-rules.pro               ← rules consumers inherit
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      ← minimal, no application tag
│       │   ├── assets/quill/
│       │   │   ├── editor.html
│       │   │   ├── editor.js
│       │   │   ├── editor.css
│       │   │   ├── quill.core.js        ← pinned to 2.0.3
│       │   │   └── quill.core.css
│       │   └── java/dev/mangolabs/quilleditor/
│       │       ├── QuillEditor.kt
│       │       ├── QuillToolbar.kt
│       │       ├── QuillState.kt
│       │       ├── QuillBridge.kt              ← internal
│       │       ├── QuillWebViewClient.kt
│       │       └── model/
│       │           ├── Delta.kt
│       │           └── Format.kt
│       ├── test/                        ← JVM unit tests (no Android dependency)
│       │   └── java/dev/mangolabs/quilleditor/...
│       └── androidTest/                 ← Instrumented tests (real WebView)
│           └── java/dev/mangolabs/quilleditor/...
│
└── sample/                              ← APP MODULE (consumes :editor as project dep)
    ├── build.gradle.kts                 ← com.android.application
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/dev/mangolabs/quilleditor/sample/
        │       ├── MainActivity.kt
        │       ├── SampleEditorScreen.kt    ← full demo: load/save Delta, image picker, dark toggle
        │       └── SampleViewModel.kt
        └── androidTest/                 ← end-to-end UI tests via Compose testing
```

**Coordinates published from `:editor`:**

- Group ID: `dev.mangolabs`
- Artifact ID: `quill-compose-editor`
- Initial version: `0.1.0-SNAPSHOT`
- Full: `dev.mangolabs:quill-compose-editor:0.1.0-SNAPSHOT`

---

## Component Architecture

```
┌─────────────────────────────────────────┐
│  QuillEditor (Composable)               │  ← Public Compose API
│   - takes QuillState                    │
│   - hosts WebView via AndroidView       │
└──────────────┬──────────────────────────┘
               │ JS bridge (both directions)
┌──────────────▼──────────────────────────┐
│  editor.html (in assets/quill/)         │  ← Web layer
│   - Single Quill instance               │
│   - Bridge object (KotlinBridge)        │
└─────────────────────────────────────────┘
```

**Data flow:**

- **Kotlin → JS:** Public methods on `QuillState` (e.g., `toggleBold()`, `insertImage(url)`, `setContent(delta)`) call `WebView.evaluateJavascript(...)` which invokes functions on `window` defined in `editor.js`.
- **JS → Kotlin:** Quill's `text-change` event (debounced 200ms) and `selection-change` event (immediate) call methods on the injected `KotlinBridge` object. The bridge marshals to the main thread and updates `QuillState`.

---

## Step 1 — Bundle Quill Assets

Place these in `editor/src/main/assets/quill/` (Quill 2.0.3):

- `quill.js` — the **full** UMD build (~209 KB). Contains all standard formats (bold, italic, underline, strike, list, header, link, image) pre-registered. Required: `quill.core.js` is bare-bones with **no** formats registered, so using it produces `Cannot register "bold"` errors at init.
- `quill.core.css` — minimum structural CSS, no theme. Pairs with our own `editor.css` for native-feel styling.

We deliberately skip the snow and bubble theme bundles because we ship our own Compose toolbar.

Pin the version in a header comment at the top of `editor.js` so future upgrades are explicit:

```javascript
// Quill v2.0.3 — do not bump without re-running the full instrumented test suite.
```

---

## Step 2 — HTML Shell

**`assets/quill/editor.html`:**

```html
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <link rel="stylesheet" href="quill.core.css">
  <link rel="stylesheet" href="editor.css">
</head>
<body>
  <div id="editor"></div>
  <script src="quill.js"></script>
  <script src="editor.js"></script>
</body>
</html>
```

---

## Step 3 — CSS (Native Feel)

**`assets/quill/editor.css`:**

```css
* { -webkit-tap-highlight-color: transparent; }

html, body {
  margin: 0;
  padding: 0;
  background: transparent;
  font-family: -apple-system, Roboto, sans-serif;
  font-size: 16px;
  line-height: 1.5;
  color: var(--text-color, #1f1f1f);
}

#editor {
  border: none;
  padding: 16px;
  min-height: 100vh;
}

.ql-editor {
  padding: 0;
  outline: none;
}

.ql-editor.ql-blank::before {
  color: var(--placeholder-color, #999);
  font-style: normal;
  left: 0;
}

/* Dark mode driven from Kotlin via CSS variables */
body.dark {
  --text-color: #e3e3e3;
  --placeholder-color: #757575;
}
```

---

## Step 4 — JS Bridge

**`assets/quill/editor.js`:**

```javascript
// Quill v2.0.3 — do not bump without re-running the full instrumented test suite.
let quill;

document.addEventListener('DOMContentLoaded', () => {
  quill = new Quill('#editor', {
    modules: {
      toolbar: false,
      history: { delay: 500, maxStack: 100 }
    },
    placeholder: 'Start writing...',
    formats: ['bold', 'italic', 'underline', 'strike', 'list', 'header', 'link', 'image']
  });

  // Push content changes to Kotlin (debounced)
  let debounceTimer;
  quill.on('text-change', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const delta = JSON.stringify(quill.getContents());
      KotlinBridge.onContentChanged(delta);
    }, 200);
  });

  // Push selection/format state to Kotlin (immediate, for toolbar sync)
  quill.on('selection-change', (range) => {
    if (range) {
      const format = quill.getFormat(range);
      KotlinBridge.onFormatChanged(JSON.stringify(format));
    } else {
      // Reset toolbar on blur / deselect so it does not show stale format
      KotlinBridge.onFormatChanged('{}');
    }
  });

  KotlinBridge.onReady();
});

// === Functions callable from Kotlin ===

window.setContents = (deltaJson) => {
  quill.setContents(JSON.parse(deltaJson), 'silent');
};

window.applyFormat = (formatName, value) => {
  const range = quill.getSelection(true);
  if (!range) return;
  if (range.length === 0) {
    quill.format(formatName, value);   // toggle for cursor position
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
```

**Design notes:**
- `text-change` is debounced 200ms — frequent updates would jank Kotlin recomposition.
- `selection-change` is **not** debounced — toolbar needs to react instantly to cursor movement. On null range (blur/deselect) we emit `'{}'` so the toolbar resets rather than holding stale state.
- `'silent'` source on `setContents` prevents loopback when Kotlin pushes state back into Quill.

---

## Step 5 — Kotlin Data Models

**`model/Delta.kt`:**

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Delta(val ops: List<DeltaOp> = emptyList())

@Serializable
data class DeltaOp(
  val insert: JsonElement? = null,    // String for text, JsonObject for embeds
  val delete: Int? = null,
  val retain: Int? = null,
  val attributes: Map<String, JsonElement>? = null
)

// Convenience extractors
val DeltaOp.textInsert: String?
  get() = (insert as? JsonPrimitive)?.contentOrNull

val DeltaOp.imageUrl: String?
  get() = (insert as? JsonObject)?.get("image")?.jsonPrimitive?.contentOrNull
```

**`model/Format.kt`:**

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ActiveFormat(
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strike: Boolean = false,
  val list: String? = null,    // "bullet" | "ordered" | null
  val header: Int? = null
) {
  companion object {
    fun fromJson(json: String): ActiveFormat {
      val obj = Json.parseToJsonElement(json).jsonObject
      // Quill sometimes emits `false` for cleared formats — treat as null.
      val listValue = obj["list"]?.jsonPrimitive
      val list = when {
        listValue == null -> null
        listValue.booleanOrNull == false -> null
        else -> listValue.contentOrNull
      }
      val headerValue = obj["header"]?.jsonPrimitive
      val header = when {
        headerValue == null -> null
        headerValue.booleanOrNull == false -> null
        else -> headerValue.intOrNull
      }
      return ActiveFormat(
        bold      = obj["bold"]?.jsonPrimitive?.booleanOrNull ?: false,
        italic    = obj["italic"]?.jsonPrimitive?.booleanOrNull ?: false,
        underline = obj["underline"]?.jsonPrimitive?.booleanOrNull ?: false,
        strike    = obj["strike"]?.jsonPrimitive?.booleanOrNull ?: false,
        list      = list,
        header    = header
      )
    }
  }
}
```

---

## Step 6 — State Holder

**`QuillState.kt`:**

```kotlin
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mangolabs.quilleditor.model.ActiveFormat
import dev.mangolabs.quilleditor.model.Delta
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Stable
class QuillState {
  var contentDelta by mutableStateOf<Delta?>(null)
    internal set

  var activeFormat by mutableStateOf(ActiveFormat())
    internal set

  var isReady by mutableStateOf(false)
    internal set

  internal var webViewRef: WebView? = null

  fun toggleBold()      = applyFormat("bold",      !activeFormat.bold)
  fun toggleItalic()    = applyFormat("italic",    !activeFormat.italic)
  fun toggleUnderline() = applyFormat("underline", !activeFormat.underline)
  fun toggleStrike()    = applyFormat("strike",    !activeFormat.strike)

  fun toggleBulletList() {
    val next: Any = if (activeFormat.list == "bullet") false else "bullet"
    applyFormat("list", next)
  }

  fun toggleOrderedList() {
    val next: Any = if (activeFormat.list == "ordered") false else "ordered"
    applyFormat("list", next)
  }

  fun setHeader(level: Int?) = applyFormat("header", level ?: false)

  fun insertImage(url: String) {
    evalJs("window.insertImage(${JSONObject.quote(url)})")
  }

  /**
   * Programmatically replaces editor contents. Because the underlying call uses
   * Quill's 'silent' source, the text-change event will not fire — so we mirror
   * the new value into [contentDelta] locally to keep Kotlin state in sync.
   */
  fun setContent(delta: Delta) {
    val json = Json.encodeToString(delta)
    evalJs("window.setContents(${JSONObject.quote(json)})")
    contentDelta = delta
  }

  fun undo()  = evalJs("window.undo()")
  fun redo()  = evalJs("window.redo()")
  fun focus() = evalJs("window.focusEditor()")

  private fun applyFormat(name: String, value: Any) {
    val jsValue = when (value) {
      is Boolean -> value.toString()
      is String  -> JSONObject.quote(value)   // escapes quotes, backslashes, control chars
      is Number  -> value.toString()
      else       -> "null"
    }
    evalJs("window.applyFormat(${JSONObject.quote(name)}, $jsValue)")
  }

  private fun evalJs(script: String) {
    webViewRef?.post { webViewRef?.evaluateJavascript(script, null) }
  }
}

@Composable
fun rememberQuillState(initial: Delta? = null): QuillState {
  return remember { QuillState().also { if (initial != null) it.contentDelta = initial } }
}
```

---

## Step 7 — JS Bridge (Kotlin Side)

**`QuillBridge.kt`:**

```kotlin
import android.webkit.JavascriptInterface
import dev.mangolabs.quilleditor.model.ActiveFormat
import dev.mangolabs.quilleditor.model.Delta
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

internal class QuillBridge(
  private val onContent: (Delta) -> Unit,
  private val onFormat: (ActiveFormat) -> Unit,
  private val onReadyCallback: () -> Unit,
  private val onMainThread: (() -> Unit) -> Unit
) {
  private val disposed = AtomicBoolean(false)
  private val json = Json { ignoreUnknownKeys = true }

  fun dispose() { disposed.set(true) }

  @JavascriptInterface
  fun onContentChanged(deltaJson: String) {
    if (disposed.get()) return
    val delta = json.decodeFromString<Delta>(deltaJson)
    onMainThread { if (!disposed.get()) onContent(delta) }
  }

  @JavascriptInterface
  fun onFormatChanged(formatJson: String) {
    if (disposed.get()) return
    val format = ActiveFormat.fromJson(formatJson)
    onMainThread { if (!disposed.get()) onFormat(format) }
  }

  @JavascriptInterface
  fun onReady() {
    if (disposed.get()) return
    onMainThread { if (!disposed.get()) onReadyCallback() }
  }
}
```

**Important:**
- `@JavascriptInterface` methods run on the WebView's JS thread, **not** the main thread. All callbacks must be marshaled via `onMainThread`.
- The bridge takes plain callbacks rather than a direct `QuillState` reference. This keeps it independent of Compose runtime and JVM-testable. `QuillEditor.kt` (Step 8) wires the callbacks to `QuillState` mutations.
- The `disposed` flag guards against the race where the WebView is torn down while a JS callback is mid-flight; without it we could mutate state on a disposed editor. The flag is checked twice — once before marshaling (cheap reject) and once inside the marshaled block (guards the race between marshal and execution).

---

## Step 8 — The Composable

**`QuillEditor.kt`:**

```kotlin
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.mangolabs.quilleditor.model.Delta
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import android.util.Log

private const val TAG = "QuillEditor"

@Composable
fun QuillEditor(
  state: QuillState,
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = isSystemInDarkTheme()
) {
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  val initialDelta = remember { state.contentDelta }
  val bridgeHolder = remember { arrayOfNulls<QuillBridge>(1) }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      WebView(ctx).apply {
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          allowFileAccess = false
          allowContentAccess = false
          mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        webChromeClient = object : WebChromeClient() {
          override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            Log.d(TAG, "[JS ${message.messageLevel()}] ${message.message()} @${message.lineNumber()}")
            return true
          }
        }

        val bridge = QuillBridge(
          onContent = { delta -> state.contentDelta = delta },
          onFormat = { format -> state.activeFormat = format },
          onReadyCallback = {
            state.isReady = true
            evaluateJavascript("window.setDarkMode($isDarkTheme)", null)
            initialDelta?.let {
              val json = Json.encodeToString(it)
              evaluateJavascript(
                "window.setContents(${JSONObject.quote(json)})", null
              )
            }
          },
          onMainThread = { block -> mainHandler.post(block) }
        )
        bridgeHolder[0] = bridge
        addJavascriptInterface(bridge, "KotlinBridge")

        webViewClient = QuillWebViewClient(ctx.filesDir)
        loadUrl("file:///android_asset/quill/editor.html")

        state.webViewRef = this
      }
    },
    update = { /* dark mode handled via LaunchedEffect below */ },
    onRelease = { webView ->
      bridgeHolder[0]?.dispose()
      state.webViewRef = null
      webView.removeJavascriptInterface("KotlinBridge")
      webView.destroy()
    }
  )

  LaunchedEffect(isDarkTheme, state.isReady) {
    if (state.isReady) {
      state.webViewRef?.evaluateJavascript("window.setDarkMode($isDarkTheme)", null)
    }
  }
}
```

**Why `LaunchedEffect` instead of `update`:** `update` runs every recomposition; `LaunchedEffect` only fires when its keys change. Saves a JS call on every unrelated state tick.

---

## Step 9 — Native Toolbar

**`QuillToolbar.kt`:**

```kotlin
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun QuillToolbar(state: QuillState, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    FormatButton(Icons.Default.FormatBold,          state.activeFormat.bold,            state::toggleBold)
    FormatButton(Icons.Default.FormatItalic,        state.activeFormat.italic,          state::toggleItalic)
    FormatButton(Icons.Default.FormatUnderlined,    state.activeFormat.underline,       state::toggleUnderline)
    FormatButton(Icons.Default.FormatStrikethrough, state.activeFormat.strike,          state::toggleStrike)
    VerticalDivider(Modifier.height(24.dp))
    FormatButton(Icons.Default.FormatListBulleted,  state.activeFormat.list == "bullet",  state::toggleBulletList)
    FormatButton(Icons.Default.FormatListNumbered,  state.activeFormat.list == "ordered", state::toggleOrderedList)
  }
}

@Composable
private fun FormatButton(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
  FilledIconToggleButton(
    checked = active,
    onCheckedChange = { onClick() },
    colors = IconButtonDefaults.filledIconToggleButtonColors(
      containerColor = Color.Transparent,
      checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer
    )
  ) { Icon(icon, contentDescription = null) }
}
```

The toolbar reacts automatically to cursor position because `activeFormat` is updated by Quill's `selection-change` event.

---

## Step 10 — Image Insertion

**Do not enable `allowFileAccess = true`.** Instead, use a custom `WebViewClient.shouldInterceptRequest` to serve local images via a synthetic scheme.

### Approach: `app-image://` synthetic URLs

1. User picks/captures an image via Compose-side launchers in the consuming app.
2. App saves it to `filesDir/images/{uuid}.jpg`.
3. App calls `quillState.insertImage("app-image://{uuid}")`.
4. The `WebViewClient` intercepts requests with scheme `app-image`, reads the file, and returns it as a `WebResourceResponse`.

**`QuillWebViewClient.kt`:**

```kotlin
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream

internal class QuillWebViewClient(private val filesDir: File) : WebViewClient() {
  override fun shouldInterceptRequest(
    view: WebView, request: WebResourceRequest
  ): WebResourceResponse? {
    val uri = request.url
    if (uri.scheme != "app-image") return null

    val id = uri.host ?: return null
    val file = File(filesDir, "images/$id.jpg")
    if (!file.exists()) return null

    // FileInputStream is closed by WebResourceResponse once the WebView finishes
    // consuming the body. Do not close it here.
    return WebResourceResponse(
      "image/jpeg",
      "UTF-8",
      FileInputStream(file)
    )
  }
}
```

### Image pickers (Compose side, lives in consuming app / sample app)

Three sources, all using Activity Result APIs (no runtime permissions on Android 13+):

- **Gallery:** `ActivityResultContracts.PickVisualMedia` with `PickVisualMediaRequest(ImageOnly)`
- **Camera:** `ActivityResultContracts.TakePicture` + `FileProvider`
- **Clipboard:** Read `ClipboardManager.primaryClip`, check `ClipData.Item.uri` for image MIME type, copy to `filesDir`

All three converge into a single `suspend fun saveImage(uri: Uri): String` that returns the UUID. The caller does `quillState.insertImage("app-image://$uuid")`.

### Image pipeline

1. Read input `Uri` via `ContentResolver.openInputStream`.
2. Decode bounds first (`BitmapFactory.Options.inJustDecodeBounds = true`).
3. Resample to max 1080px longest edge using `inSampleSize`.
4. Compress as JPEG quality 80.
5. Write to `filesDir/images/{uuid}.jpg`.

The sample app implements all three pickers as a working demo.

---

## Library Publishing

`editor/build.gradle.kts` applies:

```kotlin
plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("maven-publish")
  id("org.jetbrains.dokka")
}

android {
  namespace = "dev.mangolabs.quilleditor"
  compileSdk = 36

  defaultConfig {
    minSdk = 34
    consumerProguardFiles("consumer-rules.pro")
  }

  publishing {
    singleVariant("release") {
      withSourcesJar()
      withJavadocJar()
    }
  }
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId    = "dev.mangolabs"
      artifactId = "quill-compose-editor"
      version    = "0.1.0-SNAPSHOT"

      afterEvaluate { from(components["release"]) }

      pom {
        name.set("Quill Compose Editor")
        description.set("Jetpack Compose rich text editor backed by Quill.js.")
        url.set("https://github.com/mango-labs-dev/android-editor")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
      }
    }
  }
  repositories {
    mavenLocal()
    // Remote repository (GitHub Packages / Maven Central) wired in a later milestone.
  }
}
```

**Verification during dev:** run `./gradlew :editor:publishToMavenLocal`, then a separate scratch Android project can pull `dev.mangolabs:quill-compose-editor:0.1.0-SNAPSHOT` from `mavenLocal()` to verify the consumer experience.

`consumer-rules.pro`:

```
# Quill bridge is referenced from JavaScript via reflection — keep public surface.
-keep class dev.mangolabs.quilleditor.QuillState { *; }
-keep class dev.mangolabs.quilleditor.model.** { *; }
-keepclassmembers class dev.mangolabs.quilleditor.QuillBridge {
  @android.webkit.JavascriptInterface <methods>;
}
```

---

## TDD Test Plan

Tests are written **before** the production code in each phase. All tests live in two places: `editor/src/test/` (fast JVM) and `editor/src/androidTest/` (instrumented) for the library, and `sample/src/androidTest/` for end-to-end.

### JVM unit tests (`editor/src/test/`)

| Test class | Positive cases | Negative cases |
|---|---|---|
| `DeltaTest` | round-trip text op; round-trip image embed; round-trip attributes map; preserves unknown attribute keys | malformed JSON → throws `SerializationException`; missing `ops` key → empty list default |
| `ActiveFormatTest` | parses all formats true; parses `list: "bullet"`; parses `list: "ordered"`; parses `header: 1..6` | parses `list: false` → null; parses `header: false` → null; empty `{}` → all defaults; missing key → default |
| `DeltaOpExtractorsTest` | `textInsert` returns string; `imageUrl` returns embed src | `textInsert` returns null for embed; `imageUrl` returns null for text |

### Instrumented tests (`editor/src/androidTest/`)

| Test class | What it asserts |
|---|---|
| `QuillBridgeRoundTripTest` | Load editor, dispatch key events, `state.contentDelta` updates within 500ms |
| `SetContentRoundTripTest` | `setContent(delta)` → read back via JS → equal; `state.contentDelta` mirrored synchronously |
| `FormatSyncTest` | Toggle bold via `state.toggleBold()`, cursor format reflects bold |
| `SelectionResetTest` | Blur editor → `activeFormat` returns to defaults (toolbar resets) |
| `ImageSchemeTest` | Write fixture to `filesDir/images/UUID.jpg`, `insertImage("app-image://UUID")`, assert `<img>` present in DOM |
| `MissingImageTest` | `insertImage` for non-existent UUID → no crash, Delta still records embed |
| `LifecycleDisposeTest` | Destroy WebView while a JS callback is queued → `disposed` flag prevents state mutation, no NPE |
| `DarkModeTest` | Toggle `isDarkTheme`, `body.dark` class added/removed |
| `RapidToggleSafetyTest` | Toggle bold 10× in 200ms, final `activeFormat.bold` matches expected (debounce window respected) |

### Sample app UI tests (`sample/src/androidTest/`)

| Test class | What it asserts |
|---|---|
| `ToolbarSyncE2ETest` | Place cursor inside bold text, bold button shows checked state |
| `ConfigChangeE2ETest` | Rotate device, editor content survives (ViewModel-backed) |
| `LoadSaveE2ETest` | Type → close screen → reopen → content reloaded from sample's in-memory store |

### Acceptance criterion → test mapping

| Acceptance criterion | Test(s) |
|---|---|
| Editor renders, no web chrome | (visual; covered by sample app smoke) |
| Typing updates `contentDelta` within ~200ms | `QuillBridgeRoundTripTest` |
| Cursor movement updates `activeFormat` immediately | `FormatSyncTest`, `SelectionResetTest` |
| Toolbar reflects cursor position | `ToolbarSyncE2ETest` |
| All format toggles work | `FormatSyncTest`, `RapidToggleSafetyTest` |
| `setContent` round-trips | `SetContentRoundTripTest`, `DeltaTest` |
| Image insertion works | `ImageSchemeTest`, `MissingImageTest` |
| Dark mode propagates | `DarkModeTest` |
| Undo / redo work | `QuillBridgeRoundTripTest` (extended) |
| Rotation preserves content | `ConfigChangeE2ETest` |

---

## Usage Example (from `sample/`)

```kotlin
@Composable
fun NoteEditorScreen(loadedDelta: Delta?, onAutoSave: (Delta) -> Unit) {
  val quillState = rememberQuillState(initial = loadedDelta)

  LaunchedEffect(quillState.contentDelta) {
    quillState.contentDelta?.let { onAutoSave(it) }
  }

  Column(Modifier.fillMaxSize()) {
    QuillEditor(state = quillState, modifier = Modifier.weight(1f))
    QuillToolbar(state = quillState)
  }
}
```

The consuming app sees only `Delta` objects flowing in and out. Persistence (Room) and auto-save debouncing are the consuming app's responsibility.

---

## Build Phases

Every phase begins by writing the tests for the work it produces, then implementing until tests pass.

| Phase | Task | Estimate |
|---|---|---|
| 0 | Repo skeleton: multi-module Gradle, version catalog, `.editorconfig` (no wildcards, 2-space), LICENSE (Apache 2.0), sample app shell that renders an empty `Box`, GitHub Actions CI workflow (build + unit + instrumented matrix) | 1 day |
| 1 | Bundle Quill 2.0.3 assets; HTML/CSS shell; sample renders blank `editor.html` inside a `WebView`; smoke test loads page without errors | ½ day |
| 2 | JVM unit tests (`DeltaTest`, `ActiveFormatTest`, `DeltaOpExtractorsTest`) → then `Delta` / `ActiveFormat` models → then `QuillBridge` (with `disposed` flag) | 1 day |
| 3 | Instrumented tests (`QuillBridgeRoundTripTest`, `SetContentRoundTripTest`, `LifecycleDisposeTest`) → then `QuillState` + `QuillEditor` composable + `WebChromeClient` logging | 1 day |
| 4 | Tests (`FormatSyncTest`, `SelectionResetTest`, `RapidToggleSafetyTest`, `ToolbarSyncE2ETest`) → then `QuillToolbar` + reactive active-format wiring | ½ day |
| 5 | Tests (`ImageSchemeTest`, `MissingImageTest`) → then `QuillWebViewClient` + sample app image pickers (gallery / camera / clipboard) | 1 day |
| 6 | Tests (`DarkModeTest`, `ConfigChangeE2ETest`, `LoadSaveE2ETest`) → then polish: dark mode sync, focus handling, IME edge cases, `rememberSaveable` for config changes | 1 day |
| 7 | Wire `maven-publish` + Dokka; publish `0.1.0-SNAPSHOT` to `mavenLocal`; create a tiny scratch consumer project to verify the AAR resolves and the API is usable | ½ day |
| **Total** | | **~6½ days** |

---

## Gotchas / Things to Watch

- **Keyboard quirks:** Test with Gboard swipe typing — WebView IME composition is the most common bug source.
- **Configuration changes:** WebView state survives recomposition (factory caching in `AndroidView`), but **not** Activity recreation. Persist the current `Delta` via `rememberSaveable` or in the consuming ViewModel. The sample app demonstrates the ViewModel pattern.
- **Selection handles styling:** WebView text selection handles cannot be perfectly Material 3-styled. This is an accepted trade-off.
- **Don't base64-encode images into Delta JSON.** It bloats storage and tanks Quill performance. Use the `app-image://` scheme above.
- **Don't load Quill from a CDN.** Bundle in assets for offline reliability and zero-latency cold start.
- **Hot reload during dev:** Asset changes require app rebuild. Optionally serve from `http://10.0.2.2:8080/editor.html` during dev with `usesCleartextTraffic` on the debug manifest only.
- **Memory:** First WebView per process adds ~30–50MB resident. Acceptable for a notes app; do not create multiple WebViews simultaneously.
- **Accessibility:** TalkBack support in WebView is weaker than native Compose. If accessibility is a hard requirement, this approach is the wrong choice.
- **Quill version bump policy:** the pinned `2.0.3` is intentional. Any bump requires re-running the full instrumented test suite before merging.
- **No wildcard imports:** enforced by `.editorconfig` and ktlint. All sample code in this spec is intentionally explicit.

---

## Acceptance Criteria

### Library (`:editor`)

- [ ] `QuillEditor` composable renders an editable area, no visible web chrome (scrollbars, tap highlights, default fonts).
- [ ] Typing in the editor updates `QuillState.contentDelta` within ~200ms.
- [ ] Moving the cursor updates `QuillState.activeFormat` immediately; blur resets it.
- [ ] Toolbar buttons toggle their highlighted state based on cursor position.
- [ ] Bold / italic / underline / strikethrough / bullet list / numbered list all work.
- [ ] `setContent(delta)` round-trips: saving a Delta and re-loading produces identical rendering, and `contentDelta` reflects the new value immediately.
- [ ] Inserting an image via `insertImage("app-image://uuid")` displays the image inline and includes it in the Delta.
- [ ] Dark mode toggle propagates to the editor without reload.
- [ ] Undo / redo work via `state.undo()` / `state.redo()`.
- [ ] Disposing the WebView mid-callback does not crash (`disposed` flag).
- [ ] All JVM unit tests pass.
- [ ] All instrumented tests pass on the CI emulator.

### Sample app (`:sample`)

- [ ] Sample app builds and runs against `:editor` as a project dependency, no published artifact required.
- [ ] Sample demonstrates: load Delta, edit, save Delta, dark mode toggle, image insert from gallery/camera/clipboard.
- [ ] Rotating the device does not lose content (ViewModel-backed persistence).
- [ ] Sample app UI tests pass.

### Publishing

- [ ] `./gradlew :editor:publishToMavenLocal` succeeds.
- [ ] A scratch consumer project can resolve `dev.mangolabs:quill-compose-editor:0.1.0-SNAPSHOT` from `mavenLocal()` and use the public API.
- [ ] Sources JAR and Dokka HTML JAR are included in the published artifact.
- [ ] `consumer-rules.pro` keeps the public surface under R8.

### CI

- [ ] GitHub Actions runs `./gradlew build :editor:testDebugUnitTest` on every push.
- [ ] Instrumented tests run on the CI emulator for `:editor` and `:sample`.

---

## Dependencies (in `gradle/libs.versions.toml`)

```toml
[versions]
agp                 = "<latest stable>"
kotlin              = "<latest stable>"
compose-bom         = "<latest stable>"
activity-compose    = "<latest stable>"
kotlinx-serialization = "<latest stable>"
dokka               = "<latest stable>"

[libraries]
compose-bom          = { group = "androidx.compose", name = "compose-bom",          version.ref = "compose-bom" }
compose-material3    = { group = "androidx.compose.material3", name = "material3" }
compose-icons-ext    = { group = "androidx.compose.material",  name = "material-icons-extended" }
activity-compose     = { group = "androidx.activity", name = "activity-compose",   version.ref = "activity-compose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
android-library     = { id = "com.android.library",                 version.ref = "agp" }
android-application = { id = "com.android.application",             version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android",        version.ref = "kotlin" }
kotlin-serialization= { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
dokka               = { id = "org.jetbrains.dokka",                 version.ref = "dokka" }
```

Versions are pinned at Phase 0 — placeholders here are filled in during the repo-skeleton step.

---

## Out of Scope (for follow-up work in the notes app)

- Room schema for notes (title, Delta JSON blob, timestamps).
- Notes list screen + search.
- Auto-save debouncing in the ViewModel (sample app shows the pattern; not part of the library).
- Production image picker UI (sample app implements a minimal demo only).
- Orphan image cleanup when notes are deleted.
- Note sharing / export.
- Remote Maven repository publishing (GitHub Packages or Maven Central) — wired in a later milestone after `0.1.0` stabilises.
