package xdm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xdm.ui.screens.DownloadItem

/**
 * Single download item in the list.
 * Converted from WPF ListView GridView columns:
 *   Name | DateAdded | Size | Progress (ProgressBar) | Status
 *
 * Java Swing: custom JTable cell renderer with progress bar
 * C# WPF: GridViewColumn with DataTemplate + ProgressBar binding
 */
@Composable
fun DownloadListItem(
    item: DownloadItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // Row 1: Name + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(item.status)
            }

            Spacer(Modifier.height(4.dp))

            // Row 2: Progress bar (matches WPF ProgressBar Value binding)
            if (!item.isFinished) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }

            // Row 3: Details (size, speed, ETA, date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Size column (matches WPF GridViewColumn with FileSizeValueConverter)
                Text(
                    if (item.isFinished) item.size
                    else "${item.downloaded} / ${item.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                if (!item.isFinished) {
                    // Speed + ETA
                    Text(
                        "${item.speed} - ${item.eta}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    // Date added (matches WPF DateAdded column)
                    Text(
                        item.dateAdded,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status.lowercase()) {
        "downloading" -> MaterialTheme.colorScheme.primary
        "finished", "complete" -> MaterialTheme.colorScheme.secondary
        "failed", "error" -> MaterialTheme.colorScheme.error
        "paused", "stopped" -> MaterialTheme.colorScheme.outline
        "queued" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
