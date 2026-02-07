package xdm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xdm.ui.components.DownloadListItem
import xdm.ui.components.CategorySidebar

/**
 * Main screen - converted from WPF MainWindow.xaml (800x500, 2-column grid).
 *
 * WPF layout:
 *   Left: ListBox (190px) with category icons + labels
 *   Right: DockPanel with toolbar, search, ListView, status bar
 *
 * Java Swing equivalent: MainWindow.java (JFrame + JTable + JToolBar)
 *
 * Compose: Row { CategorySidebar | Column { TopBar, SearchBar, DownloadList, StatusBar } }
 */

data class DownloadItem(
    val id: String,
    val name: String,
    val url: String,
    val size: String,
    val downloaded: String,
    val progress: Float, // 0..1
    val speed: String,
    val eta: String,
    val status: String,
    val dateAdded: String,
    val isFinished: Boolean = false,
)

enum class Category(val label: String) {
    All("All Downloads"),
    Incomplete("Incomplete"),
    Finished("Finished"),
    Compressed("Compressed"),
    Documents("Documents"),
    Music("Music"),
    Video("Videos"),
    Programs("Programs"),
    Other("Other"),
}

@Composable
fun MainScreen(
    downloads: List<DownloadItem> = emptyList(),
    selectedCategory: Category = Category.All,
    onCategorySelected: (Category) -> Unit = {},
    onAddDownload: () -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onOpenFile: (String) -> Unit = {},
    onOpenFolder: (String) -> Unit = {},
    onSettings: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    monitoringEnabled: Boolean = true,
    onToggleMonitoring: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }

    Row(Modifier.fillMaxSize()) {
        // Left sidebar - Category navigation (190px in WPF)
        CategorySidebar(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.width(200.dp).fillMaxHeight(),
        )

        // Main content area
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Toolbar (matches WPF DockPanel top toolbar)
            TopToolbar(
                onAdd = onAddDownload,
                onDelete = { selectedId?.let { onDeleteDownload(it) } },
                onPause = { selectedId?.let { onPauseDownload(it) } },
                onResume = { selectedId?.let { onResumeDownload(it) } },
                onOpenFile = { selectedId?.let { onOpenFile(it) } },
                onOpenFolder = { selectedId?.let { onOpenFolder(it) } },
                onSettings = onSettings,
                hasSelection = selectedId != null,
            )

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    onSearch(it)
                },
            )

            // Download list (matches WPF ListView with GridView columns)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadListItem(
                        item = item,
                        isSelected = item.id == selectedId,
                        onClick = { selectedId = item.id },
                        onDoubleClick = { onOpenFile(item.id) },
                    )
                }

                if (downloads.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No downloads",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // Status bar (matches WPF bottom Grid)
            StatusBar(
                monitoringEnabled = monitoringEnabled,
                onToggleMonitoring = onToggleMonitoring,
                downloadCount = downloads.size,
            )
        }
    }
}

@Composable
private fun TopToolbar(
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
    onSettings: () -> Unit,
    hasSelection: Boolean,
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Maps to WPF toolbar buttons: New, Delete, Open, OpenFolder, Pause, Resume
            ToolbarButton(Icons.Default.Add, "New", onClick = onAdd)
            ToolbarButton(Icons.Default.Delete, "Delete", onClick = onDelete, enabled = hasSelection)

            Spacer(Modifier.width(8.dp))

            ToolbarButton(Icons.Default.OpenInNew, "Open", onClick = onOpenFile, enabled = hasSelection)
            ToolbarButton(Icons.Default.FolderOpen, "Folder", onClick = onOpenFolder, enabled = hasSelection)

            Spacer(Modifier.width(8.dp))

            ToolbarButton(Icons.Default.Pause, "Pause", onClick = onPause, enabled = hasSelection)
            ToolbarButton(Icons.Default.PlayArrow, "Resume", onClick = onResume, enabled = hasSelection)

            Spacer(Modifier.weight(1f))

            ToolbarButton(Icons.Default.Settings, "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    // Matches WPF TextBox + placeholder TextBlock + clear Button
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search downloads...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatusBar(
    monitoringEnabled: Boolean,
    onToggleMonitoring: () -> Unit,
    downloadCount: Int,
) {
    // Matches WPF bottom status bar Grid
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Monitoring toggle (WPF: StatusText with toggle)
            FilterChip(
                selected = monitoringEnabled,
                onClick = onToggleMonitoring,
                label = {
                    Text(
                        if (monitoringEnabled) "Monitoring ON" else "Monitoring OFF",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        if (monitoringEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )

            Spacer(Modifier.weight(1f))

            Text(
                "$downloadCount downloads",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}
