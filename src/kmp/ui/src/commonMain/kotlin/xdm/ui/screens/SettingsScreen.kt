package xdm.ui.screens

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
import androidx.compose.ui.unit.dp

/**
 * Settings screen.
 * Converted from WPF SettingsWindow.xaml (700x500).
 *
 * WPF: ListBox sidebar (categories) + swappable UserControl content area
 *   - BrowserMonitoringView
 *   - GeneralSettingsView
 *   - NetworkSettingsView
 *   - PasswordManagerView
 *   - AdvancedSettingsView
 *
 * Java Swing: SettingsPage.java (JTabbedPane)
 */

enum class SettingsTab(val label: String) {
    BrowserMonitoring("Browser Monitoring"),
    General("General"),
    Network("Network"),
    Credentials("Credentials"),
    Advanced("Advanced"),
}

@Composable
fun SettingsScreen(
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.General) }

    Row(Modifier.fillMaxSize()) {
        // Settings category sidebar (matches WPF ListBox)
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
        ) {
            SettingsTab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = tab == selectedTab,
                    onClick = { selectedTab = tab },
                    icon = {
                        Icon(
                            when (tab) {
                                SettingsTab.BrowserMonitoring -> Icons.Default.Language
                                SettingsTab.General -> Icons.Default.Tune
                                SettingsTab.Network -> Icons.Default.Wifi
                                SettingsTab.Credentials -> Icons.Default.Key
                                SettingsTab.Advanced -> Icons.Default.Build
                            },
                            contentDescription = tab.label,
                        )
                    },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // Content area
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(16.dp),
        ) {
            // Tab content
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    SettingsTab.BrowserMonitoring -> BrowserMonitoringSettings()
                    SettingsTab.General -> GeneralSettings()
                    SettingsTab.Network -> NetworkSettings()
                    SettingsTab.Credentials -> CredentialSettings()
                    SettingsTab.Advanced -> AdvancedSettings()
                }
            }

            // OK / Cancel buttons
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave) { Text("Save") }
            }
        }
    }
}

// -- BrowserMonitoringView (from WPF BrowserMonitoringView.xaml) --

@Composable
private fun BrowserMonitoringSettings() {
    var fileTypes by remember { mutableStateOf("3GP 7Z AVI BZ2 DEB DOC DOCX EXE GZ ISO MSI PDF RAR RPM RUN TAR XZ ZIP") }
    var minVideoSize by remember { mutableStateOf("1 MB") }
    var monitorClipboard by remember { mutableStateOf(false) }
    var fetchTimestamp by remember { mutableStateOf(true) }
    var blockedSites by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Browser Extensions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {}) { Text("Chrome") }
                OutlinedButton(onClick = {}) { Text("Firefox") }
                OutlinedButton(onClick = {}) { Text("Edge") }
                OutlinedButton(onClick = {}) { Text("Opera") }
            }
        }
        item {
            OutlinedTextField(
                value = fileTypes,
                onValueChange = { fileTypes = it },
                label = { Text("Monitored file types") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Min video size:", modifier = Modifier.width(120.dp))
                // Simplified - would use dropdown in real implementation
                OutlinedTextField(
                    value = minVideoSize,
                    onValueChange = { minVideoSize = it },
                    singleLine = true,
                    modifier = Modifier.width(120.dp),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = monitorClipboard, onCheckedChange = { monitorClipboard = it })
                Text("Monitor clipboard")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = fetchTimestamp, onCheckedChange = { fetchTimestamp = it })
                Text("Fetch server timestamp")
            }
        }
        item {
            OutlinedTextField(
                value = blockedSites,
                onValueChange = { blockedSites = it },
                label = { Text("Blocked sites (one per line)") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
            )
        }
    }
}

// -- GeneralSettingsView (from WPF GeneralSettingsView.xaml) --

@Composable
private fun GeneralSettings() {
    var showProgressWindow by remember { mutableStateOf(true) }
    var showCompleteWindow by remember { mutableStateOf(true) }
    var maxParallel by remember { mutableStateOf("5") }
    var tempFolder by remember { mutableStateOf("/tmp") }
    var downloadFolder by remember { mutableStateOf("~/Downloads") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("General", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showProgressWindow, onCheckedChange = { showProgressWindow = it })
                Text("Show download progress window")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showCompleteWindow, onCheckedChange = { showCompleteWindow = it })
                Text("Show download complete window")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Max parallel downloads:", modifier = Modifier.width(180.dp))
                OutlinedTextField(
                    value = maxParallel,
                    onValueChange = { maxParallel = it },
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                )
            }
        }
        item {
            OutlinedTextField(
                value = tempFolder,
                onValueChange = { tempFolder = it },
                label = { Text("Temporary folder") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {}) { Icon(Icons.Default.FolderOpen, "Browse") }
                },
            )
        }
        item {
            OutlinedTextField(
                value = downloadFolder,
                onValueChange = { downloadFolder = it },
                label = { Text("Download folder") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {}) { Icon(Icons.Default.FolderOpen, "Browse") }
                },
            )
        }
    }
}

// -- NetworkSettingsView (from WPF NetworkSettingsView.xaml) --

@Composable
private fun NetworkSettings() {
    var maxSegments by remember { mutableStateOf("8") }
    var timeout by remember { mutableStateOf("30") }
    var speedLimitEnabled by remember { mutableStateOf(false) }
    var speedLimit by remember { mutableStateOf("0") }
    var proxyType by remember { mutableStateOf(0) }
    var proxyHost by remember { mutableStateOf("") }
    var proxyPort by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Network", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Max segments:", modifier = Modifier.width(140.dp))
                OutlinedTextField(
                    value = maxSegments, onValueChange = { maxSegments = it },
                    singleLine = true, modifier = Modifier.width(80.dp),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Timeout (sec):", modifier = Modifier.width(140.dp))
                OutlinedTextField(
                    value = timeout, onValueChange = { timeout = it },
                    singleLine = true, modifier = Modifier.width(80.dp),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = speedLimitEnabled, onCheckedChange = { speedLimitEnabled = it })
                Text("Speed limit (KB/s):")
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = speedLimit, onValueChange = { speedLimit = it },
                    singleLine = true, modifier = Modifier.width(100.dp),
                    enabled = speedLimitEnabled,
                )
            }
        }
        item {
            Text("Proxy", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type:", modifier = Modifier.width(80.dp))
                listOf("None", "System", "HTTP", "SOCKS5").forEachIndexed { i, label ->
                    FilterChip(
                        selected = proxyType == i,
                        onClick = { proxyType = i },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
        }
        if (proxyType >= 2) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proxyHost, onValueChange = { proxyHost = it },
                        label = { Text("Host") }, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = proxyPort, onValueChange = { proxyPort = it },
                        label = { Text("Port") }, modifier = Modifier.width(80.dp),
                    )
                }
            }
        }
    }
}

// -- PasswordManagerView --

@Composable
private fun CredentialSettings() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Saved Credentials", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Credentials for password-protected downloads",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Add") }
                OutlinedButton(onClick = {}) { Text("Remove") }
            }
        }
    }
}

// -- AdvancedSettingsView --

@Composable
private fun AdvancedSettings() {
    var haltSystem by remember { mutableStateOf(false) }
    var keepAwake by remember { mutableStateOf(false) }
    var runCommand by remember { mutableStateOf(false) }
    var customCommand by remember { mutableStateOf("") }
    var runAntivirus by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Advanced", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = haltSystem, onCheckedChange = { haltSystem = it })
                Text("Shutdown system after all downloads complete")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = keepAwake, onCheckedChange = { keepAwake = it })
                Text("Prevent system sleep during downloads")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = runCommand, onCheckedChange = { runCommand = it })
                Text("Run command after download")
            }
        }
        if (runCommand) {
            item {
                OutlinedTextField(
                    value = customCommand, onValueChange = { customCommand = it },
                    label = { Text("Command") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = runAntivirus, onCheckedChange = { runAntivirus = it })
                Text("Run antivirus scan after download")
            }
        }
    }
}
