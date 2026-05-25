# Rules consumers of this library inherit via consumerProguardFiles.
# The Quill JS bridge calls @JavascriptInterface methods reflectively, so the
# bridge class and its public API surface must survive R8.

-keep class dev.mangolabs.quilleditor.QuillState { *; }
-keep class dev.mangolabs.quilleditor.model.** { *; }
-keepclassmembers class dev.mangolabs.quilleditor.QuillBridge {
  @android.webkit.JavascriptInterface <methods>;
}
