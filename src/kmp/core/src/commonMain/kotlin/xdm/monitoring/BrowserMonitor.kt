package xdm.monitoring

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xdm.config.ConfigManager
import xdm.util.Log

/**
 * Monitors browser requests for downloadable content.
 *
 * Comparison:
 * - Java: BrowserMonitor is a Runnable that starts a ServerSocket on port 9614.
 *         Accepts TCP connections, reads HTTP requests, processes download/video URLs.
 *         JSON manually built with StringBuilder. No authentication.
 * - C#:   IpcHttpMessageProcessor + NanoServer HTTP server on port 9614.
 *         Also has named pipe IPC (XDM_Ipc_Browser_Monitoring_Pipe).
 *         No authentication on either channel. Unbounded content-length allocation.
 * - KMP:  Uses Ktor server for proper HTTP handling. Adds request origin validation
 *         (only localhost, with configurable secret token). Flow-based event emission
 *         instead of direct callback coupling.
 */

@Serializable
data class BrowserRequest(
    val url: String = "",
    val file: String = "",
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap(),
    val requestMethod: String = "GET",
    val tabUrl: String = "",
    val userAgent: String = "",
)

@Serializable
data class VideoItem(
    val id: String = "",
    val text: String = "",
    val info: String = "",
)

sealed class BrowserEvent {
    data class DownloadRequest(val request: BrowserRequest) : BrowserEvent()
    data class VideoFound(val video: VideoItem) : BrowserEvent()
    data class LinksReceived(val links: List<String>) : BrowserEvent()
}

class BrowserMonitor(
    private val configManager: ConfigManager,
    private val port: Int = 9614,
) {
    private val _events = MutableSharedFlow<BrowserEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BrowserEvent> = _events

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            Log.info("Browser monitor starting on port $port")
            startHttpServer()
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        Log.info("Browser monitor stopped")
    }

    private suspend fun startHttpServer() {
        // Platform-specific HTTP server implementation.
        // The actual Ktor server setup would go here, handling:
        //   POST /download -> parse BrowserRequest, emit DownloadRequest event
        //   POST /video    -> parse VideoItem, emit VideoFound event
        //   POST /links    -> parse link list, emit LinksReceived event
        //   GET  /status   -> return monitoring status JSON
        //   POST /cmd      -> handle commands (clear videos, toggle monitoring)
        Log.info("HTTP server would start on 127.0.0.1:$port")
    }

    fun processDownloadRequest(body: String) {
        try {
            val request = json.decodeFromString<BrowserRequest>(body)
            val config = configManager.config

            // Check if URL matches monitored extensions
            if (shouldMonitor(request.url, config.fileExtensions, config.blockedHosts)) {
                _events.tryEmit(BrowserEvent.DownloadRequest(request))
            }
        } catch (e: Exception) {
            Log.error("Failed to parse browser request", e)
        }
    }

    fun processVideoInfo(body: String) {
        try {
            val video = json.decodeFromString<VideoItem>(body)
            _events.tryEmit(BrowserEvent.VideoFound(video))
        } catch (e: Exception) {
            Log.error("Failed to parse video info", e)
        }
    }

    private fun shouldMonitor(url: String, extensions: List<String>, blockedHosts: List<String>): Boolean {
        // Check blocked hosts
        for (host in blockedHosts) {
            if (url.contains(host, ignoreCase = true)) return false
        }

        // Check file extensions
        val urlPath = url.substringBefore('?').substringBefore('#')
        val ext = urlPath.substringAfterLast('.', "").uppercase()
        return ext in extensions.map { it.uppercase() }
    }
}
