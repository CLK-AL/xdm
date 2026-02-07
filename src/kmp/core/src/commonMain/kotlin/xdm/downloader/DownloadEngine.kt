package xdm.downloader

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xdm.model.*
import xdm.util.Helpers
import xdm.util.Log

/**
 * Core download engine - the shared heart of XDM.
 *
 * Architecture comparison:
 * - Java: Downloader abstract class with start/stop/resume, segments managed via SegmentDownloader.
 *         Each segment runs in its own Thread. Segments are split/merged based on download progress.
 * - C#:   HTTPDownloaderBase with Piece/PieceGrabber hierarchy. Uses ReaderWriterLockSlim for
 *         concurrent access. PieceGrabber runs downloads on ThreadPool threads.
 * - KMP:  Uses coroutines instead of raw threads. Flow-based progress reporting.
 *         Mutex instead of ReaderWriterLock (no recursive lock needed since coroutines are
 *         cooperative). Structured concurrency ensures proper cleanup.
 */

// -- Events emitted by the engine --

sealed class DownloadEvent {
    data class Started(val id: String) : DownloadEvent()
    data class Probed(val id: String, val size: Long, val resumable: Boolean) : DownloadEvent()
    data class Progress(val id: String, val progress: ProgressInfo) : DownloadEvent()
    data class Finished(val id: String, val path: String) : DownloadEvent()
    data class Failed(val id: String, val error: DownloadError) : DownloadEvent()
    data class Cancelled(val id: String) : DownloadEvent()
    data class AssemblyProgress(val id: String, val percent: Int) : DownloadEvent()
}

data class ProgressInfo(
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSecond: Long = 0L,
    val progress: Int = 0,
    val eta: String = "--",
    val activeSegments: Int = 0,
)

data class DownloadError(
    val code: ErrorCode = ErrorCode.Generic,
    val message: String = "",
)

enum class ErrorCode {
    Generic,
    NetworkError,
    FileError,
    HttpError,
    AuthRequired,
    NotFound,
    DiskFull,
    Cancelled,
}

// -- Segment downloader interface --

interface SegmentDownloader {
    suspend fun probe(metadata: DownloadMetadata): ProbeResult
    suspend fun downloadSegment(
        metadata: DownloadMetadata,
        segment: Segment,
        outputPath: String,
        onProgress: (bytesDownloaded: Long) -> Unit,
    )
}

data class ProbeResult(
    val totalSize: Long = -1L,
    val resumable: Boolean = false,
    val fileName: String? = null,
    val contentType: String? = null,
    val redirectedUrl: String? = null,
)

/**
 * Manages a single download operation with multiple concurrent segments.
 *
 * Key improvements over Java/C#:
 * - Structured concurrency: all segment downloads are children of the download scope.
 *   Cancelling the download automatically cancels all segments.
 * - No raw threads or manual synchronization for stop/cancel.
 * - Flow-based progress: UI observes a StateFlow instead of polling or callback registration.
 * - Proper backpressure via conflated flows.
 */
class Download(
    val entry: DownloadEntry,
    private val segmentDownloader: SegmentDownloader,
    private val tempDir: String,
    private val outputDir: String,
) {
    private val mutex = Mutex()
    private var segments = mutableListOf<Segment>()
    private var job: Job? = null
    private var totalSize: Long = -1L
    private var resumable: Boolean = false

    private val _events = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DownloadEvent> = _events

    private val _progress = MutableStateFlow(ProgressInfo())
    val progress: StateFlow<ProgressInfo> = _progress

    private var downloadStartTime = 0L
    private var totalDownloadedBytes = 0L

    suspend fun start() {
        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                _events.emit(DownloadEvent.Started(entry.id))

                // Phase 1: Probe
                val probeResult = segmentDownloader.probe(
                    HttpDownloadMetadata(url = entry.primaryUrl, headers = emptyMap())
                )
                totalSize = probeResult.totalSize
                resumable = probeResult.resumable

                if (probeResult.fileName != null) {
                    entry.file = Helpers.sanitizeFileName(probeResult.fileName)
                }
                entry.size = totalSize

                _events.emit(DownloadEvent.Probed(entry.id, totalSize, resumable))

                // Phase 2: Create segments
                mutex.withLock {
                    segments = createSegments(totalSize, resumable, entry.maxSegments)
                }

                downloadStartTime = Helpers.tickCount()

                // Phase 3: Download all segments concurrently
                val segmentJobs = segments.map { segment ->
                    launch {
                        downloadSingleSegment(segment)
                    }
                }

                // Wait for all segments
                segmentJobs.joinAll()

                // Phase 4: Assemble
                if (segments.all { it.isFinished }) {
                    _events.emit(DownloadEvent.AssemblyProgress(entry.id, 0))
                    val outputPath = assembleSegments()
                    _events.emit(DownloadEvent.AssemblyProgress(entry.id, 100))
                    _events.emit(DownloadEvent.Finished(entry.id, outputPath))
                } else {
                    val failedSegment = segments.firstOrNull { it.state == SegmentState.Failed }
                    _events.emit(
                        DownloadEvent.Failed(
                            entry.id,
                            DownloadError(ErrorCode.Generic, "Segment ${failedSegment?.id} failed")
                        )
                    )
                }
            } catch (e: CancellationException) {
                _events.emit(DownloadEvent.Cancelled(entry.id))
            } catch (e: Exception) {
                Log.error("Download ${entry.id} failed", e)
                _events.emit(
                    DownloadEvent.Failed(entry.id, DownloadError(ErrorCode.Generic, e.message ?: "Unknown error"))
                )
            }
        }
    }

    suspend fun stop() {
        job?.cancelAndJoin()
        mutex.withLock {
            segments.filter { it.isActive }.forEach { it.state = SegmentState.Stopped }
        }
    }

    suspend fun resume() {
        // Re-use existing segments, only restart non-finished ones
        mutex.withLock {
            segments.filter { it.state != SegmentState.Finished }.forEach {
                it.state = SegmentState.NotStarted
            }
        }
        start()
    }

    private suspend fun downloadSingleSegment(segment: Segment) {
        val segmentFile = "$tempDir/${entry.id}_${segment.id}.part"

        segment.state = SegmentState.Downloading
        try {
            segmentDownloader.downloadSegment(
                metadata = HttpDownloadMetadata(
                    url = entry.primaryUrl,
                    headers = emptyMap(),
                ),
                segment = segment,
                outputPath = segmentFile,
            ) { bytesDownloaded ->
                mutex.withLock {
                    segment.downloaded = bytesDownloaded
                    updateProgress()
                }
            }
            segment.state = SegmentState.Finished
        } catch (e: CancellationException) {
            segment.state = SegmentState.Stopped
            throw e
        } catch (e: Exception) {
            segment.state = SegmentState.Failed
            Log.error("Segment ${segment.id} failed", e)
        }
    }

    private fun updateProgress() {
        totalDownloadedBytes = segments.sumOf { it.downloaded }
        val elapsed = (Helpers.tickCount() - downloadStartTime).coerceAtLeast(1)
        val speed = totalDownloadedBytes * 1000 / elapsed
        val percent = if (totalSize > 0) ((totalDownloadedBytes * 100) / totalSize).toInt() else 0
        val etaSeconds = if (speed > 0 && totalSize > 0) {
            (totalSize - totalDownloadedBytes) / speed
        } else -1L
        val activeCount = segments.count { it.isActive }

        _progress.value = ProgressInfo(
            downloadedBytes = totalDownloadedBytes,
            totalBytes = totalSize,
            speedBytesPerSecond = speed,
            progress = percent.coerceIn(0, 100),
            eta = Helpers.formatEta(etaSeconds),
            activeSegments = activeCount,
        )
        _events.tryEmit(DownloadEvent.Progress(entry.id, _progress.value))
    }

    private suspend fun assembleSegments(): String {
        val outputPath = "$outputDir/${entry.file}"
        // Assembly logic would merge segment files into the final output.
        // For now, return the path.
        Log.info("Assembling ${segments.size} segments into $outputPath")
        return outputPath
    }

    companion object {
        fun createSegments(totalSize: Long, resumable: Boolean, maxSegments: Int): MutableList<Segment> {
            if (totalSize <= 0 || !resumable) {
                return mutableListOf(
                    Segment(id = "s0", startOffset = 0, length = totalSize)
                )
            }

            val count = maxSegments.coerceIn(1, 32)
            val segmentSize = totalSize / count
            return (0 until count).map { i ->
                val offset = i * segmentSize
                val length = if (i == count - 1) totalSize - offset else segmentSize
                Segment(
                    id = "s$i",
                    startOffset = offset,
                    length = length,
                )
            }.toMutableList()
        }
    }
}
