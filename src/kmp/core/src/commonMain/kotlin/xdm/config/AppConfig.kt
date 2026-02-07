package xdm.config

import kotlinx.serialization.Serializable
import xdm.model.ProxyInfo
import xdm.model.AuthCredential

/**
 * Application configuration.
 * Unified from Java Config.java and C# Config.cs.
 *
 * Java: singleton with getters/setters, saved to custom binary format
 * C#:  singleton with properties, saved via ConfigIO with custom binary format
 * KMP: data class with JSON serialization for portability
 */
@Serializable
data class AppConfig(
    val maxSegments: Int = 8,
    val maxRetries: Int = 10,
    val retryDelaySeconds: Int = 5,
    val networkTimeoutSeconds: Int = 30,
    val maxConcurrentDownloads: Int = 5,
    val downloadFolder: String = "",
    val tempFolder: String = "",
    val showNotification: Boolean = true,
    val monitorClipboard: Boolean = false,
    val monitorBrowser: Boolean = true,
    val speedLimitKbps: Int = 0,
    val showDownloadWindow: Boolean = true,
    val showDownloadCompleteWindow: Boolean = true,
    val proxy: ProxyInfo? = null,
    val credentials: List<AuthCredential> = emptyList(),
    val fileExtensions: List<String> = DEFAULT_EXTENSIONS,
    val videoExtensions: List<String> = DEFAULT_VIDEO_EXTENSIONS,
    val blockedHosts: List<String> = emptyList(),
    val executeCommandAfterCompletion: Boolean = false,
    val afterCompletionCommand: String = "",
    val autoShutdown: Boolean = false,
    val duplicateAction: DuplicateAction = DuplicateAction.Ask,
    val language: String = "en",
    val isBrowserMonitorEnabled: Boolean = true,
    val fetchServerTimestamp: Boolean = true,
    val keepTempFiles: Boolean = false,
    val categorizationEnabled: Boolean = true,
) {
    companion object {
        val DEFAULT_EXTENSIONS = listOf(
            "3GP", "7Z", "AVI", "BZ2", "DEB", "DOC", "DOCX", "EXE", "GZ", "ISO",
            "MSI", "PDF", "RAR", "RPM", "RUN", "TAR", "XZ", "ZIP",
            "MP3", "FLAC", "AAC", "WAV", "WMA", "OGG",
            "MP4", "MKV", "WEBM", "FLV", "MOV", "AVI", "WMV",
            "APK", "JAR", "DMG", "IMG",
        )
        val DEFAULT_VIDEO_EXTENSIONS = listOf(
            "MP4", "MKV", "WEBM", "FLV", "MOV", "AVI", "WMV", "3GP", "M4V",
        )
    }
}

@Serializable
enum class DuplicateAction {
    Ask,
    Overwrite,
    AutoRename,
}
