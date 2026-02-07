package xdm.downloader

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xdm.model.*
import xdm.util.Helpers
import xdm.util.Log
import java.io.*
import java.util.*

/**
 * Direct Kotlin conversion of Java SegmentDownloader.java.
 * Uses coroutines instead of raw Threads, Mutex instead of synchronized.
 *
 * Java original: xdman.downloaders.SegmentDownloader (626 lines)
 * This is a faithful port preserving the same segment split/merge/retry logic.
 */
abstract class SegmentDownloaderKt(
    val id: String,
    protected val folder: String,
    protected val config: xdm.config.AppConfig,
) {
    @Volatile
    protected var stopFlag = false
    protected var length: Long = -1L
    protected var downloaded: Long = 0L
    protected var lastDownloaded: Long = 0L
    protected var prevTime: Long = System.currentTimeMillis()
    protected var progress: Int = 0
    protected var finished: Boolean = false
    protected var assembling: Boolean = false
    protected var converting: Boolean = false
    protected var convertPrg: Int = 0
    protected var downloadSpeed: Float = 0f
    protected var eta: String = "---"
    protected var lastModified: String? = null
    protected var errorCode: Int = 0
    protected var outputFormat: Int = 0
    protected var lastSavedAt: Long = 0L
    protected var lastUpdatedAt: Long = 0L

    private val mutex = Mutex()
    private var init = false
    private var assembleFinished = false
    private var totalAssembled = 0L
    private val minChunkSize = config.maxSegments.coerceAtLeast(256 * 1024)
    protected val maxCount = config.maxSegments

    protected val chunks = mutableListOf<SegmentKt>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var listener: DownloadListenerKt? = null

    abstract fun createChannel(segment: SegmentKt): AbstractChannelKt

    open fun onChunkConfirmed(segment: SegmentKt) {}

    // -- Start --

    fun start() {
        Log.info("Creating folder $folder")
        File(folder).mkdirs()
        chunks.clear()
        try {
            val c1 = SegmentKt(folder = folder)
            c1.length = -1
            c1.startOffset = 0
            c1.downloaded = 0
            chunks.add(c1)
            launchSegmentDownload(c1)
        } catch (e: IOException) {
            errorCode = ErrorCode.NetworkError.ordinal
            listener?.downloadFailed(id)
        }
    }

    // -- Resume --

    fun resume() {
        stopFlag = false
        Log.info("Resuming download $id")
        if (!restoreState()) {
            Log.info("No saved state, starting from beginning")
            start()
            return
        }
        lastDownloaded = downloaded
        prevTime = System.currentTimeMillis()
        init = true

        val c1 = findInactiveChunk()
        if (c1 != null) {
            launchSegmentDownload(c1)
        } else if (allFinished()) {
            scope.launch { assembleAsync() }
        } else {
            Log.error("No inactive/incomplete chunk found while resuming")
        }
    }

    // -- Stop --

    fun stop() {
        stopFlag = true
        saveState()
        chunks.forEach { it.stop() }
        scope.cancel()
        listener?.downloadStopped(id)
    }

    // -- Segment lifecycle callbacks --

    suspend fun chunkInitiated(segmentId: String) {
        if (stopFlag) return
        mutex.withLock {
            if (!init) {
                val c = getById(segmentId) ?: return
                length = c.length
                init = true
                Log.info("Download size: $length")
                saveState()
                onChunkConfirmed(c)
                listener?.downloadConfirmed(id)
            }
            if (length > 0) {
                createChunk()
            }
        }
    }

    suspend fun chunkComplete(segmentId: String): Boolean {
        if (finished || stopFlag) return true
        mutex.withLock {
            saveState()
            return onComplete(segmentId)
        }
    }

    fun chunkUpdated(segmentId: String) {
        if (stopFlag) return
        val now = System.currentTimeMillis()
        if (now - lastSavedAt > 5000) {
            saveState()
            lastSavedAt = now
        }
        if (now - lastUpdatedAt > 1000) {
            updateStatus()
            lastUpdatedAt = now
            retryFailedIfNeeded()
        }
    }

    fun chunkFailed(segmentId: String, reason: String) {
        if (stopFlag) return
        Log.error("Segment $segmentId failed: $reason")
        val segment = getById(segmentId) ?: return
        segment.state = SegmentState.Failed
        retryFailedIfNeeded()
    }

    // -- Chunk management (ported from Java synchronized methods) --

    private fun createChunk() {
        if (stopFlag) return
        val activeCount = getActiveChunkCount()
        if (activeCount >= maxCount) return

        var remaining = maxCount - activeCount
        remaining -= retryFailedChunks(remaining)

        if (remaining > 0) {
            val maxChunk = findMaxChunk() ?: return
            val newChunk = splitChunk(maxChunk) ?: return
            Log.info("Creating new chunk ${newChunk.id}")
            chunks.add(newChunk)
            launchSegmentDownload(newChunk)
        }
    }

    private fun findMaxChunk(): SegmentKt? {
        if (stopFlag) return null
        var maxSize = -1L
        var maxId: String? = null
        for (c in chunks) {
            if (c.isActive) {
                val rem = c.length - c.downloaded
                if (rem > maxSize) {
                    maxId = c.id
                    maxSize = rem
                }
            }
        }
        if (maxSize < minChunkSize) return null
        return maxId?.let { getById(it) }
    }

    private fun splitChunk(c: SegmentKt?): SegmentKt? {
        if (c == null || stopFlag) return null
        val rem = c.length - c.downloaded
        val offset = c.startOffset + c.length - rem / 2
        val len = rem / 2
        c.length -= rem / 2

        return SegmentKt(folder = folder).apply {
            length = len
            startOffset = offset
        }
    }

    private fun mergeChunk(c1: SegmentKt, c2: SegmentKt) {
        c1.length += c2.length
    }

    private fun findNextNeedyChunk(chunk: SegmentKt): SegmentKt? {
        if (stopFlag) return null
        val offset = chunk.startOffset + chunk.length
        return chunks.firstOrNull {
            it.downloaded == 0L && !it.isFinished && it.startOffset == offset
        }
    }

    private fun onComplete(segmentId: String): Boolean {
        if (allFinished() || length < 0) {
            finished = true
            updateStatus()
            scope.launch { assembleAsync() }
            return true
        }

        val chunk = getById(segmentId) ?: return true
        val needy = findNextNeedyChunk(chunk)
        if (needy != null) {
            needy.stop()
            chunks.remove(needy)
            needy.dispose()
            mergeChunk(chunk, needy)
            createChunk()
            return false
        }
        chunk.clearChannel()
        createChunk()
        return true
    }

    // -- Assembly --

    private suspend fun assembleAsync() {
        finished = true
        try {
            assemble()
            if (!assembleFinished) throw IOException("Assembly failed")
            Log.info("Download $id finished")
            updateStatus()
            cleanup()
            listener?.downloadFinished(id)
        } catch (e: Exception) {
            if (!stopFlag) {
                Log.error("Assembly failed for $id", e)
                errorCode = ErrorCode.FileError.ordinal
                listener?.downloadFailed(id)
            }
        }
    }

    private fun assemble() {
        totalAssembled = 0L
        assembling = true
        assembleFinished = false

        val outFile = File(folder, "${UUID.randomUUID()}_output")
        try {
            val buf = ByteArray(1024 * 1024)
            chunks.sortBy { it.startOffset }

            FileOutputStream(outFile).use { out ->
                for (c in chunks) {
                    if (stopFlag) return
                    FileInputStream(File(folder, c.id)).use { input ->
                        var rem = c.length
                        while (true) {
                            val toRead = if (rem > 0) minOf(rem, buf.size.toLong()).toInt() else buf.size
                            val r = input.read(buf, 0, toRead)
                            if (stopFlag) return
                            if (r == -1) {
                                if (length > 0) throw IOException("Unexpected EOF during assembly")
                                else break
                            }
                            out.write(buf, 0, r)
                            if (length > 0) {
                                rem -= r
                                if (rem == 0L) break
                            }
                            totalAssembled += r
                            val now = System.currentTimeMillis()
                            if (now - lastUpdatedAt > 1000) {
                                updateStatus()
                                lastUpdatedAt = now
                            }
                        }
                    }
                }
            }
            assembleFinished = true
        } catch (e: Exception) {
            Log.error("Assembly error", e)
            if (!assembleFinished) outFile.delete()
            throw IOException(e)
        }
    }

    // -- State persistence (ported from Java text format) --

    private fun saveState() {
        if (length < 0) return
        try {
            val sb = StringBuilder()
            sb.appendLine(length)
            sb.appendLine(downloaded)
            sb.appendLine(chunks.size)
            for (seg in chunks) {
                sb.appendLine(seg.id)
                sb.appendLine(seg.length)
                sb.appendLine(seg.startOffset)
                sb.appendLine(seg.downloaded)
            }
            lastModified?.let { sb.appendLine(it) }

            val tmp = File(folder, "${System.currentTimeMillis()}.tmp")
            val out = File(folder, "state.txt")
            tmp.writeText(sb.toString())
            out.delete()
            tmp.renameTo(out)
        } catch (e: Exception) {
            Log.error("Failed to save state", e)
        }
    }

    private fun restoreState(): Boolean {
        chunks.clear()
        val file = File(folder, "state.txt")
        if (!file.exists()) return false
        try {
            BufferedReader(FileReader(file)).use { br ->
                length = br.readLine().toLong()
                downloaded = br.readLine().toLong()
                val count = br.readLine().toInt()
                for (i in 0 until count) {
                    val cid = br.readLine().trim()
                    val len = br.readLine().trim().toLong()
                    val off = br.readLine().trim().toLong()
                    val dwn = br.readLine().trim().toLong()
                    val seg = SegmentKt(folder = folder, existingId = cid).apply {
                        length = len
                        startOffset = off
                        this.downloaded = dwn
                    }
                    chunks.add(seg)
                }
                lastModified = br.readLine()
            }
            return true
        } catch (e: Exception) {
            Log.error("Failed to restore state", e)
            return false
        }
    }

    // -- Helpers --

    private fun getById(segmentId: String): SegmentKt? = chunks.firstOrNull { it.id == segmentId }
    private fun getActiveChunkCount(): Int = chunks.count { it.isActive }
    private fun allFinished(): Boolean = chunks.all { it.isFinished }
    private fun findInactiveChunk(): SegmentKt? = chunks.firstOrNull { !it.isFinished && !it.isActive }

    private fun retryFailedChunks(maxRetries: Int): Int {
        var retried = 0
        for (c in chunks) {
            if (retried >= maxRetries) break
            if (c.state == SegmentState.Failed) {
                c.state = SegmentState.NotStarted
                c.reopenStream()
                launchSegmentDownload(c)
                retried++
            }
        }
        return retried
    }

    private fun retryFailedIfNeeded() {
        val active = getActiveChunkCount()
        if (active < maxCount) {
            retryFailedChunks(maxCount - active)
        }
    }

    private fun updateStatus() {
        try {
            val now = System.currentTimeMillis()
            if (assembling) {
                val len = if (length > 0) length else downloaded
                progress = if (len > 0) ((totalAssembled * 100) / len).toInt() else 0
            } else {
                var dl = 0L
                downloadSpeed = 0f
                for (s in chunks) {
                    dl += s.downloaded
                    downloadSpeed += s.transferRate
                }
                downloaded = dl
                if (length > 0) {
                    progress = ((downloaded * 100) / length).toInt()
                    val diff = downloaded - lastDownloaded
                    val timeSpent = now - prevTime
                    if (timeSpent > 0) {
                        val rate = (diff.toFloat() / timeSpent) * 1000
                        eta = Helpers.formatEta(
                            if (rate > 0) ((length - downloaded) / rate).toLong() else -1
                        )
                        lastDownloaded = downloaded
                        prevTime = now
                    }
                }
            }
            listener?.downloadUpdated(id)
        } catch (e: Exception) {
            Log.error("Status update error", e)
        }
    }

    private fun cleanup() {
        try {
            File(folder).listFiles()?.forEach { it.delete() }
            File(folder).delete()
        } catch (e: Exception) {
            Log.error("Cleanup error", e)
        }
    }

    private fun launchSegmentDownload(segment: SegmentKt) {
        scope.launch {
            try {
                val channel = createChannel(segment)
                segment.channel = channel
                segment.state = SegmentState.Downloading
                channel.open()
            } catch (e: Exception) {
                if (!stopFlag) {
                    segment.state = SegmentState.Failed
                    Log.error("Segment ${segment.id} launch failed", e)
                }
            }
        }
    }
}

// -- Listener interface (matches Java DownloadListener) --

interface DownloadListenerKt {
    fun downloadConfirmed(id: String)
    fun downloadUpdated(id: String)
    fun downloadFinished(id: String)
    fun downloadFailed(id: String)
    fun downloadStopped(id: String)
}

// -- Segment (Kotlin version of Java SegmentImpl) --

class SegmentKt(
    val folder: String,
    val id: String = UUID.randomUUID().toString(),
    existingId: String? = null,
) {
    val actualId: String = existingId ?: id
    var length: Long = -1L
    var startOffset: Long = 0L

    @Volatile
    var downloaded: Long = 0L

    @Volatile
    var state: SegmentState = SegmentState.NotStarted

    @Volatile
    var channel: AbstractChannelKt? = null

    private var outStream: RandomAccessFile? = null
    private var bytesRead1: Long = 0L
    private var time1: Long = System.currentTimeMillis()
    var transferRate: Float = 0f
        private set

    val isFinished: Boolean get() = length >= 0 && downloaded >= length
    val isActive: Boolean get() = channel != null && state == SegmentState.Downloading

    init {
        if (existingId != null) {
            // Resuming: open existing file and seek
            outStream = RandomAccessFile(File(folder, existingId), "rw").apply {
                seek(downloaded)
            }
        } else {
            outStream = RandomAccessFile(File(folder, id), "rw")
        }
    }

    fun getOutStream(): RandomAccessFile? = outStream

    fun writeBytes(data: ByteArray, offset: Int, len: Int) {
        outStream?.write(data, offset, len)
        downloaded += len
        calculateTransferRate()
    }

    fun stop() {
        state = SegmentState.Stopped
        dispose()
    }

    fun dispose() {
        channel?.stop()
        channel = null
        try { outStream?.close() } catch (_: Exception) {}
        outStream = null
    }

    fun clearChannel() {
        channel = null
    }

    fun reopenStream() {
        if (outStream != null) return
        outStream = RandomAccessFile(File(folder, actualId), "rw").apply {
            seek(downloaded)
        }
    }

    private fun calculateTransferRate() {
        val now = System.currentTimeMillis()
        val timeDiff = now - time1
        val bytesDiff = downloaded - bytesRead1
        if (timeDiff > 1000) {
            transferRate = (bytesDiff.toFloat() / timeDiff) * 1000
            bytesRead1 = downloaded
            time1 = now
        }
    }
}

// -- Abstract channel (matches Java AbstractChannel) --

abstract class AbstractChannelKt {
    abstract fun open()
    abstract fun stop()
}
