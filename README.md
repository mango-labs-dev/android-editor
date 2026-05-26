# Quill Compose Editor

A Jetpack Compose rich text editor backed by Quill.js. The toolbar and surrounding chrome are 100 % Compose / Material 3; only the content area is a `WebView`. Consumers see a clean `QuillState` + `QuillEditor` API and persist content as Quill Delta JSON.

- **Library module:** `:editor` — publishes the AAR.
- **Sample app:** `:sample` — consumes the library via `project(":editor")` so you can iterate without a publish step.

Status: `0.1.0`. Published to GitHub Packages (and `mavenLocal` for development).

---

## Install

### 1. Authenticate to GitHub Packages

Add a personal access token with the `read:packages` scope to `~/.gradle/gradle.properties` (not the project's):

```properties
gpr.user=<your-github-username>
gpr.token=<PAT with read:packages>
```

### 2. Declare the repository

In your project's `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven.pkg.github.com/mango-labs-dev/android-editor")
      credentials {
        username = providers.gradleProperty("gpr.user").orNull
        password = providers.gradleProperty("gpr.token").orNull
      }
    }
  }
}
```

### 3. Depend on it

```kotlin
dependencies {
  implementation("dev.mangolabs:quill-compose-editor:0.1.0")
}
```

The library transitively brings in Compose UI, Material 3, the extended icons set, and `kotlinx-serialization-json`.

---

## Quick start

```kotlin
@Composable
fun MyNoteEditor() {
  val state = rememberQuillState()

  // Autosave: observe contentDelta in your ViewModel / repository.
  LaunchedEffect(state.contentDelta) {
    state.contentDelta?.let { vm.save(it) }
  }

  Column(Modifier.fillMaxSize()) {
    QuillEditor(state = state, modifier = Modifier.weight(1f))
    QuillToolbar(state = state)
  }
}
```

Programmatic mutations all live on `QuillState`:

```kotlin
state.toggleBold()
state.setHeader(level = 2)
state.toggleBulletList()
state.setSelection(index = 0, length = 5)
state.insertImage("app-image://my-uuid")
state.setContent(deltaFromDisk)
state.undo()
state.redo()
```

---

## Features

- **Quill Delta as the storage format** — round-trips cleanly through `kotlinx.serialization`.
- **Native Compose toolbar** — bold / italic / underline / strikethrough / bullet list / numbered list, with active-format state driven from the editor's cursor.
- **Image insertion via a synthetic `app-image://` scheme** — the library intercepts requests at `WebViewClient.shouldInterceptRequest` and serves bytes from `filesDir/images/<uuid>.jpg`. No base64 in the Delta; no `allowFileAccess`.
- **Dark mode** — flip the `isDarkTheme` parameter and the editor's CSS variables re-skin without a reload.
- **Offline-first** — Quill 2.0.3 is bundled in `assets/`. No CDN, no cold-start latency.
- **Bridge dispose flag** — guards against the race where a JS callback is in flight while the WebView is being torn down.
- **JVM-testable bridge** — `QuillBridge` takes plain callbacks, so the Delta / ActiveFormat parsing logic is covered by fast unit tests rather than instrumented ones.

---

## Sample app

The `:sample` module demonstrates the recommended consumer patterns:

- ViewModel-backed autosave that survives `Activity` recreation.
- Outlined Material 3 frame around the editor (the library composable itself stays chrome-free).
- Image picker bottom sheet with three sources — gallery (`PickVisualMedia`), camera (`TakePicture` + `FileProvider`), and clipboard.
- IME-aware layout via `Modifier.imePadding()`.

Run it with `./gradlew :sample:installDebug` after starting an emulator or attaching a device.

---

## Build & test

```bash
# Compile both modules
./gradlew assemble

# JVM unit tests
./gradlew :editor:test

# Instrumented tests (needs a running emulator / device)
./gradlew :editor:connectedDebugAndroidTest :sample:connectedDebugAndroidTest

# Publish a snapshot to ~/.m2 for offline-consumer testing
./gradlew :editor:publishToMavenLocal
```

CI runs unit tests on every push and instrumented tests on every push against an API 34 google_apis emulator.

---

## Release

Releases are tag-driven. Push a `v*` tag and the `Release` workflow publishes that version to GitHub Packages:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Or fire it manually via the **Actions → Release → Run workflow** button and supply a version. The version flows in as `-PpublishVersion=<tag>` so the local `0.1.0-SNAPSHOT` in `editor/build.gradle.kts` is overridden at publish time.

---

## Design spec

The full implementation spec lives at [`quill-compose-editor-spec.md`](./quill-compose-editor-spec.md) — it covers the JS bridge contract, the Delta/ActiveFormat data model, the `app-image://` scheme, the TDD test plan, and the phased build history.

---

## License

Apache 2.0 — see [`LICENSE`](./LICENSE).
