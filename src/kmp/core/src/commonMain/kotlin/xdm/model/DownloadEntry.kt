package xdm.model

import kotlinx.serialization.Serializable

/**
 * Represents a download item in the download list.
 * Unified from Java DownloadEntry and C# InProgressDownloadItem/FinishedDownloadItem.
 */
@Serializable
data class DownloadEntry(
    val id: String,
    var file: String = "",
    var folder: String = "",
    var state: DownloadState = DownloadState.Stopped,
    var category: FileCategory = FileCategory.Other,
    var size: Long = -1L,
    var downloaded: Long = 0L,
    var dateAdded: Long = 0L,
    var progress: Int = 0,
    var queueId: String? = null,
    var startedByUser: Boolean = false,
    var outputFormatIndex: Int = 0,
    var tempFolder: String = "",
    var downloadType: DownloadType = DownloadType.Http,
    var primaryUrl: String = "",
    var authentication: AuthCredential? = null,
    var proxyInfo: ProxyInfo? = null,
    var maxSegments: Int = 8,
)

@Serializable
enum class DownloadState {
    Queued,
    Downloading,
    Stopped,
    Finished,
    Failed,
    Assembling,
    Converting,
}

@Serializable
enum class DownloadType {
    Http,
    Ftp,
    Dash,
    Hls,
    Hds,
}

@Serializable
enum class FileCategory {
    Compressed,
    Documents,
    Music,
    Video,
    Programs,
    Other,
}

@Serializable
data class AuthCredential(
    val host: String = "",
    val userName: String = "",
    val password: String = "",
)

@Serializable
data class ProxyInfo(
    val type: ProxyType = ProxyType.Direct,
    val host: String = "",
    val port: Int = 0,
    val userName: String = "",
    val password: String = "",
)

@Serializable
enum class ProxyType {
    Direct,
    Http,
    Socks5,
    System,
}
