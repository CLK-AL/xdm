package xdm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * New download dialog.
 * Converted from WPF NewDownloadWindow.xaml (550x300).
 *
 * WPF layout:
 *   Address: [TextBox] [FileIcon]
 *   File:    [TextBox]
 *   Save In: [ComboBox]     [Size Display]
 *   [More]  [Download Later] [Download Now]
 *
 * Java Swing: NewDownloadWindow.java (JDialog with JTextField + JComboBox)
 */
@Composable
fun NewDownloadScreen(
    url: String = "",
    onUrlChange: (String) -> Unit = {},
    fileName: String = "",
    onFileNameChange: (String) -> Unit = {},
    folders: List<String> = listOf("Downloads"),
    selectedFolderIndex: Int = 0,
    onFolderSelected: (Int) -> Unit = {},
    fileSize: String = "",
    onDownload: () -> Unit = {},
    onDownloadLater: () -> Unit = {},
    onAdvanced: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    Card(
        modifier = Modifier.width(560.dp).padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("New Download", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            // Address field (matches WPF TxtUrl)
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { /* paste from clipboard */ }) {
                        Icon(Icons.Default.ContentPaste, "Paste")
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // File name field (matches WPF TxtFile)
            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChange,
                label = { Text("File Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Save location + file size (matches WPF CmbLocation + TxtFileSize)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = folders.getOrElse(selectedFolderIndex) { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Save In") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        folders.forEachIndexed { index, folder ->
                            DropdownMenuItem(
                                text = { Text(folder) },
                                onClick = {
                                    onFolderSelected(index)
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                if (fileSize.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        fileSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Buttons (matches WPF: [More] [Download Later] [Download Now])
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onAdvanced) { Text("More...") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDownloadLater) { Text("Download Later") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onDownload) { Text("Download Now") }
            }
        }
    }
}
