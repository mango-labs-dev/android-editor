package dev.mangolabs.quilleditor

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

private const val LABEL_BOLD = "Bold"
private const val LABEL_ITALIC = "Italic"
private const val LABEL_UNDERLINE = "Underline"
private const val LABEL_STRIKETHROUGH = "Strikethrough"
private const val LABEL_BULLET_LIST = "Bullet list"
private const val LABEL_NUMBERED_LIST = "Numbered list"

@Composable
fun QuillToolbar(state: QuillState, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    FormatButton(
      icon = Icons.Default.FormatBold,
      label = LABEL_BOLD,
      active = state.activeFormat.bold,
      onClick = state::toggleBold
    )
    FormatButton(
      icon = Icons.Default.FormatItalic,
      label = LABEL_ITALIC,
      active = state.activeFormat.italic,
      onClick = state::toggleItalic
    )
    FormatButton(
      icon = Icons.Default.FormatUnderlined,
      label = LABEL_UNDERLINE,
      active = state.activeFormat.underline,
      onClick = state::toggleUnderline
    )
    FormatButton(
      icon = Icons.Default.FormatStrikethrough,
      label = LABEL_STRIKETHROUGH,
      active = state.activeFormat.strike,
      onClick = state::toggleStrike
    )
    VerticalDivider(modifier = Modifier.height(24.dp))
    FormatButton(
      icon = Icons.Default.FormatListBulleted,
      label = LABEL_BULLET_LIST,
      active = state.activeFormat.list == "bullet",
      onClick = state::toggleBulletList
    )
    FormatButton(
      icon = Icons.Default.FormatListNumbered,
      label = LABEL_NUMBERED_LIST,
      active = state.activeFormat.list == "ordered",
      onClick = state::toggleOrderedList
    )
  }
}

@Composable
private fun FormatButton(
  icon: ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit
) {
  FilledIconToggleButton(
    checked = active,
    onCheckedChange = { onClick() },
    colors = IconButtonDefaults.filledIconToggleButtonColors(
      containerColor = Color.Transparent,
      checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer
    )
  ) {
    Icon(imageVector = icon, contentDescription = label)
  }
}
