package xdm.downloader

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xdm.config.ConfigManager
import xdm.model.*
import xdm.util.Helpers
import xdm.util.Log

/**
 * Top-level download manager. Coordinates all active downloads,
 * queues, and persistence.
 *
 * Comparison:
 * - Java XDMApp: monolithic god-class handling downloads, UI, browser monitoring, queues,
 *   and config all in one ~800 line class. Uses HashMap<String, Downloader> with no locking.
 * - C# ApplicationCore: better separated but still has inconsistent locking on liveDownloads
 *   dictionary. Uses lock(this) in some event handlers but not others.
 * - KMP: Clean separation. Mutex-protected concurrent map. Coroutine scope per download.
 *   Flow-based event distribution to UI layer.
 */
class DownloadManager(
    private val configManager: ConfigManager,
    private val segmentDownloaderFactory: SegmentDownloaderFactory,
) {
    private val mutex = Mutex()
    private val activeDownloads = mutableMapOf<String, Download>()
    private val downloadEntries = mutableMapOf<String, DownloadEntry>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _events = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<DownloadEvent> = _events

    suspend fun startDownload(
        url: String,
        fileName: String? = null,
        folder: String? = null,
        type: DownloadType = DownloadType.Http,
        headers: Map<String, List<String>> = emptyMap(),
        auth: AuthCredential? = null,
    ): String {
        val config = configManager.config
        val id = Helpers.generateId()
        val entry = DownloadEntry(
            id = id,
            file = fileName ?: Helpers.getFileNameFromUrl(url),
            folder = folder ?: config.downloadFolder,
            state = DownloadState.Queued,
            category = Helpers.categorizeFile(fileName ?: Helpers.getFileNameFromUrl(url)),
            dateAdded = Helpers.tickCount(),
            downloadType = type,
            primaryUrl = url,
            authentication = auth,
            maxSegments = config.maxSegments,
            tempFolder = config.tempFolder,
        )

        mutex.withLock {
            downloadEntries[id] = entry

            if (activeDownloads.size >= config.maxConcurrentDownloads) {
                entry.state = DownloadState.Queued
                Log.info("Download $id queued (max concurrent reached)")
                return id
            }
        }

        launchDownload(entry)
        return id
    }

    suspend fun stopDownload(id: String) {
        mutex.withLock {
            activeDownloads[id]?.let { download ->
                download.stop()
                activeDownloads.remove(id)
                downloadEntries[id]?.state = DownloadState.Stopped
            }
        }
        processQueue()
    }

    suspend fun resumeDownload(id: String) {
        val entry = mutex.withLock { downloadEntries[id] } ?: return
        if (entry.state == DownloadState.Stopped || entry.state == DownloadState.Failed) {
            launchDownload(entry)
        }
    }

    suspend fun cancelDownload(id: String) {
        mutex.withLock {
            activeDownloads[id]?.let { download ->
                download.stop()
                activeDownloads.remove(id)
                downloadEntries.remove(id)
            }
        }
        processQueue()
    }

    suspend fun stopAll() {
        mutex.withLock {
            activeDownloads.values.forEach { it.stop() }
            activeDownloads.clear()
        }
    }

    fun getEntry(id: String): DownloadEntry? = downloadEntries[id]

    fun getAllEntries(): List<DownloadEntry> = downloadEntries.values.toList()

    fun isActive(id: String): Boolean = activeDownloads.containsKey(id)

    private suspend fun launchDownload(entry: DownloadEntry) {
        val segmentDownloader = segmentDownloaderFactory.create(entry.downloadType)
        val download = Download(
            entry = entry,
            segmentDownloader = segmentDownloader,
            tempDir = entry.tempFolder,
            outputDir = entry.folder,
        )

        mutex.withLock {
            activeDownloads[entry.id] = download
            entry.state = DownloadState.Downloading
        }

        // Forward events
        scope.launch {
            download.events.collect { event ->
                _events.emit(event)
                when (event) {
                    is DownloadEvent.Finished -> onDownloadComplete(entry.id)
                    is DownloadEvent.Failed -> onDownloadFailed(entry.id)
                    is DownloadEvent.Cancelled -> onDownloadComplete(entry.id)
                    else -> {}
                }
            }
        }

        download.start()
    }

    private suspend fun onDownloadComplete(id: String) {
        mutex.withLock {
            activeDownloads.remove(id)
            downloadEntries[id]?.state = DownloadState.Finished
        }
        processQueue()
    }

    private suspend fun onDownloadFailed(id: String) {
        mutex.withLock {
            activeDownloads.remove(id)
            downloadEntries[id]?.state = DownloadState.Failed
        }
        processQueue()
    }

    private suspend fun processQueue() {
        val config = configManager.config
        mutex.withLock {
            val queued = downloadEntries.values
                .filter { it.state == DownloadState.Queued }
                .sortedBy { it.dateAdded }

            for (entry in queued) {
                if (activeDownloads.size >= config.maxConcurrentDownloads) break
                scope.launch { launchDownload(entry) }
            }
        }
    }
}

fun interface SegmentDownloaderFactory {
    fun create(type: DownloadType): SegmentDownloader
}
