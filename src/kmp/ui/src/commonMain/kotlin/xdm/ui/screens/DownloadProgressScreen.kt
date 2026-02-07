package xdm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Download progress dialog.
 * Converted from WPF DownloadProgressWindow.xaml (450x280).
 *
 * WPF layout:
 *   Top: Icon | Filename + URL
 *   Middle: Status, ProgressBar, Speed, ETA, SpeedLimit
 *   Bottom: [Hide] [Stop] [Pause]
 *
 * Java Swing: DownloadWindow.java (JDialog with JProgressBar)
 */
@Composable
fun DownloadProgressScreen(
    fileName: String = "",
    url: String = "",
    status: String = "Starting...",
    progress: Float = 0f,
    speed: String = "0 B/s",
    eta: String = "--",
    downloaded: String = "0 B",
    totalSize: String = "Unknown",
    speedLimitEnabled: Boolean = false,
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onHide: () -> Unit = {},
    onSpeedLimit: () -> Unit = {},
) {
    Card(
        modifier = Modifier.width(480.dp).padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            // File info header (matches WPF top Border)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Status row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(status, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$downloaded / $totalSize",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar (matches WPF PrgProgress)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            // Speed + ETA row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Speed: $speed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    "ETA: $eta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                TextButton(onClick = onSpeedLimit) {
                    Text(
                        if (speedLimitEnabled) "Speed Limit: ON" else "Speed Limit",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Button row (matches WPF bottom: [Hide] [Stop] [Pause])
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onHide) { Text("Hide") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onStop) { Text("Stop") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onPause) { Text("Pause") }
            }
        }
    }
}
