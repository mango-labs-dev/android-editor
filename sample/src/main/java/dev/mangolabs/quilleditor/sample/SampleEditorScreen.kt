package dev.mangolabs.quilleditor.sample

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import dev.mangolabs.quilleditor.QuillEditor
import dev.mangolabs.quilleditor.QuillToolbar
import dev.mangolabs.quilleditor.rememberQuillState
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "QuillSample"

/**
 * Image-picker launchers and any pending state are hoisted here so they
 * survive [ImagePickerSheet] being dismissed before the chosen activity
 * returns its result. Each launcher posts the saved image's app-image://
 * URL to [dev.mangolabs.quilleditor.QuillState.insertImage].
 */
@Composable
fun SampleEditorScreen(modifier: Modifier = Modifier) {
  val state = rememberQuillState()
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  val store = remember { ImageStore(ctx) }

  var showPicker by remember { mutableStateOf(false) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        runCatching { store.saveImage(uri) }
          .onSuccess { id -> state.insertImage("app-image://$id") }
          .onFailure { e -> Log.e(TAG, "gallery save failed", e) }
      }
    }
  }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    val uri = pendingCameraUri
    pendingCameraUri = null
    if (success && uri != null) {
      scope.launch {
        runCatching { store.saveImage(uri) }
          .onSuccess { id -> state.insertImage("app-image://$id") }
          .onFailure { e -> Log.e(TAG, "camera save failed", e) }
      }
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    QuillEditor(
      state = state,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    )
    HorizontalDivider()
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      QuillToolbar(state = state, modifier = Modifier.weight(1f))
      IconButton(onClick = { showPicker = true }) {
        Icon(
          imageVector = Icons.Default.AddPhotoAlternate,
          contentDescription = "Insert image"
        )
      }
    }
  }

  ImagePickerSheet(
    visible = showPicker,
    onDismiss = { showPicker = false },
    onGalleryClick = {
      showPicker = false
      galleryLauncher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
      )
    },
    onCameraClick = {
      showPicker = false
      val cacheFile = File(ctx.cacheDir, "camera-${UUID.randomUUID()}.jpg").apply {
        parentFile?.mkdirs()
      }
      val uri = FileProvider.getUriForFile(
        ctx, "${ctx.packageName}.fileprovider", cacheFile
      )
      pendingCameraUri = uri
      cameraLauncher.launch(uri)
    },
    onClipboardClick = {
      showPicker = false
      val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val uri = cm.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.uri
      if (uri == null) {
        Toast.makeText(ctx, "No image on clipboard", Toast.LENGTH_SHORT).show()
        return@ImagePickerSheet
      }
      scope.launch {
        runCatching { store.saveImage(uri) }
          .onSuccess { id -> state.insertImage("app-image://$id") }
          .onFailure { e -> Log.e(TAG, "clipboard save failed", e) }
      }
    }
  )
}
