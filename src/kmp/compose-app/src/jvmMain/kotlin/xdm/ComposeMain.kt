package xdm

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import xdm.ui.screens.*
import xdm.ui.theme.XdmTheme

/**
 * Desktop Compose Multiplatform entry point.
 *
 * This replaces:
 * - Java: Main.java -> XDMApp.start() -> MainWindow (Swing JFrame)
 * - C# WPF: App.xaml.cs -> MainWindow.xaml (WPF Window)
 * - C# GTK: Program.cs -> MainWindow.cs (GTK ApplicationWindow)
 *
 * Single codebase for Windows, Linux, macOS via Compose Desktop.
 * Can also compile to native via GraalVM native-image.
 */
fun main() = application {
    var showSettings by remember { mutableStateOf(false) }
    var showNewDownload by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(Category.All) }
    var monitoringEnabled by remember { mutableStateOf(true) }

    // Sample data for demonstration
    val downloads = remember {
        mutableStateListOf(
            DownloadItem("1", "ubuntu-24.04-desktop-amd64.iso", "https://releases.ubuntu.com/...",
                "4.7 GB", "2.1 GB", 0.45f, "12.5 MB/s", "3m 28s", "Downloading", "2024-02-07"),
            DownloadItem("2", "kotlin-compiler-2.1.0.zip", "https://github.com/JetBrains/...",
                "85.2 MB", "85.2 MB", 1.0f, "", "", "Finished", "2024-02-06", isFinished = true),
        )
    }

    // Main Window (matches WPF 800x500)
    Window(
        onCloseRequest = ::exitApplication,
        title = "XDM 2024",
        state = rememberWindowState(size = DpSize(900.dp, 600.dp)),
    ) {
        XdmTheme {
            MainScreen(
                downloads = downloads,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onAddDownload = { showNewDownload = true },
                onSettings = { showSettings = true },
                monitoringEnabled = monitoringEnabled,
                onToggleMonitoring = { monitoringEnabled = !monitoringEnabled },
            )
        }
    }

    // Settings dialog
    if (showSettings) {
        Window(
            onCloseRequest = { showSettings = false },
            title = "Settings",
            state = rememberWindowState(size = DpSize(750.dp, 550.dp)),
        ) {
            XdmTheme {
                SettingsScreen(
                    onSave = { showSettings = false },
                    onCancel = { showSettings = false },
                )
            }
        }
    }

    // New download dialog
    if (showNewDownload) {
        Window(
            onCloseRequest = { showNewDownload = false },
            title = "New Download",
            state = rememberWindowState(size = DpSize(580.dp, 350.dp)),
        ) {
            XdmTheme {
                NewDownloadScreen(
                    onDownload = { showNewDownload = false },
                    onDownloadLater = { showNewDownload = false },
                    onDismiss = { showNewDownload = false },
                )
            }
        }
    }
}
