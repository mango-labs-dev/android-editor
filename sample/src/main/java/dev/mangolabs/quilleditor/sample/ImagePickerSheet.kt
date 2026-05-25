package dev.mangolabs.quilleditor.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pure presentation. Launchers and any pending state must live in the parent
 * so they survive this sheet being dismissed before the chosen activity
 * returns a result. Each source button calls its corresponding callback and
 * dismisses the sheet immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSheet(
  visible: Boolean,
  onDismiss: () -> Unit,
  onGalleryClick: () -> Unit,
  onCameraClick: () -> Unit,
  onClipboardClick: () -> Unit
) {
  if (!visible) return

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = "Insert image",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )
      ListItem(
        headlineContent = { Text("From gallery") },
        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onGalleryClick)
      )
      ListItem(
        headlineContent = { Text("Take photo") },
        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onCameraClick)
      )
      ListItem(
        headlineContent = { Text("From clipboard") },
        leadingContent = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClipboardClick)
      )
    }
  }
}
