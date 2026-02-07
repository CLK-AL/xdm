package xdm.model

import kotlinx.serialization.Serializable

/**
 * Download metadata types.
 * Unified from Java HttpMetadata/DashMetadata/HlsMetadata/HdsMetadata
 * and C# SingleSourceHTTPDownloaderState/MultiSourceDownloadState.
 */
@Serializable
sealed class DownloadMetadata {
    abstract val url: String
    abstract val headers: Map<String, List<String>>
    abstract val cookies: Map<String, String>
}

@Serializable
data class HttpDownloadMetadata(
    override val url: String,
    override val headers: Map<String, List<String>> = emptyMap(),
    override val cookies: Map<String, String> = emptyMap(),
    val contentLength: Long = -1L,
    val acceptRanges: Boolean = false,
    val contentType: String? = null,
    val lastModified: String? = null,
) : DownloadMetadata()

@Serializable
data class DashDownloadMetadata(
    override val url: String,
    override val headers: Map<String, List<String>> = emptyMap(),
    override val cookies: Map<String, String> = emptyMap(),
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val duration: Long = 0L,
) : DownloadMetadata()

@Serializable
data class HlsDownloadMetadata(
    override val url: String,
    override val headers: Map<String, List<String>> = emptyMap(),
    override val cookies: Map<String, String> = emptyMap(),
    val playlistUrl: String = "",
    val segments: List<HlsSegmentInfo> = emptyList(),
    val encryptionKey: ByteArray? = null,
    val iv: ByteArray? = null,
) : DownloadMetadata() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HlsDownloadMetadata) return false
        return url == other.url && playlistUrl == other.playlistUrl
    }

    override fun hashCode(): Int = url.hashCode() * 31 + playlistUrl.hashCode()
}

@Serializable
data class HlsSegmentInfo(
    val url: String,
    val duration: Double = 0.0,
)

@Serializable
data class HdsDownloadMetadata(
    override val url: String,
    override val headers: Map<String, List<String>> = emptyMap(),
    override val cookies: Map<String, String> = emptyMap(),
    val manifestUrl: String = "",
) : DownloadMetadata()

@Serializable
data class FtpDownloadMetadata(
    override val url: String,
    override val headers: Map<String, List<String>> = emptyMap(),
    override val cookies: Map<String, String> = emptyMap(),
    val userName: String = "",
    val password: String = "",
) : DownloadMetadata()
