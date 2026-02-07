package xdm

import kotlinx.coroutines.runBlocking
import xdm.config.AppConfig
import xdm.config.ConfigManager
import xdm.downloader.DownloadManager
import xdm.downloader.SegmentDownloaderFactory
import xdm.model.DownloadType
import xdm.util.FileSystem
import xdm.util.Log
import xdm.util.PlatformInfo

/**
 * Native entry point for XDM.
 * When compiled with GraalVM native-image, this produces a single executable.
 *
 * Usage:
 *   ./xdm                          # Start with browser monitoring
 *   ./xdm --url <url>              # Download a URL directly
 *   ./xdm --url <url> -o <file>    # Download with output filename
 */
fun main(args: Array<String>) {
    Log.info("XDM 8.0.0 (Kotlin/Native via GraalVM)")
    Log.info("Platform: ${PlatformInfo.os} ${PlatformInfo.arch}")

    val fs = FileSystem()
    val configDir = "${PlatformInfo.appDataDir}/xdm"
    val configManager = ConfigManager(fs, configDir)
    configManager.load()

    // Parse CLI args
    val argMap = parseArgs(args)

    if (argMap.containsKey("url")) {
        // CLI download mode
        val url = argMap["url"]!!
        val output = argMap["o"] ?: argMap["output"]
        Log.info("Downloading: $url")

        runBlocking {
            val manager = DownloadManager(configManager, stubFactory())
            val id = manager.startDownload(url, fileName = output)
            Log.info("Download started: $id")
            // In real implementation, would wait for completion
        }
    } else {
        // GUI/daemon mode
        Log.info("Starting XDM daemon with browser monitoring...")
        val app = XdmApplication(stubFactory())
        app.initialize()

        // Keep running
        Log.info("XDM running. Press Ctrl+C to stop.")
        Runtime.getRuntime().addShutdownHook(Thread {
            app.shutdown()
        })
        Thread.currentThread().join()
    }
}

private fun parseArgs(args: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        if (arg.startsWith("--") || arg.startsWith("-")) {
            val key = arg.removePrefix("--").removePrefix("-")
            val value = if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                i++
                args[i]
            } else "true"
            map[key] = value
        }
        i++
    }
    return map
}

// Stub factory for now - real implementation would create HTTP/HLS/DASH segment downloaders
private fun stubFactory() = SegmentDownloaderFactory { type ->
    object : xdm.downloader.SegmentDownloader {
        override suspend fun probe(metadata: xdm.model.DownloadMetadata) =
            xdm.downloader.ProbeResult(totalSize = -1)

        override suspend fun downloadSegment(
            metadata: xdm.model.DownloadMetadata,
            segment: xdm.model.Segment,
            outputPath: String,
            onProgress: (Long) -> Unit,
        ) {
            // Real implementation would use Ktor HTTP client
        }
    }
}
