package xdm

import xdm.config.ConfigManager
import xdm.downloader.DownloadManager
import xdm.downloader.SegmentDownloaderFactory
import xdm.model.DownloadType
import xdm.monitoring.BrowserMonitor
import xdm.queue.QueueManager
import xdm.util.FileSystem
import xdm.util.Log
import xdm.util.PlatformInfo

/**
 * Main application entry point, wiring together all components.
 *
 * Conversion from Java XDMApp.java / C# ApplicationCore.cs:
 * - Java XDMApp: ~800 line god-class, singleton, mixes concerns
 * - C# ApplicationCore: better separation but inconsistent locking
 * - KMP: Clean composition, no singleton, dependency injection
 */
class XdmApplication(
    private val segmentDownloaderFactory: SegmentDownloaderFactory,
) {
    val fs = FileSystem()
    val configDir = "${PlatformInfo.appDataDir}/xdm"
    val configManager = ConfigManager(fs, configDir)
    val downloadManager = DownloadManager(configManager, segmentDownloaderFactory)
    val browserMonitor = BrowserMonitor(configManager)
    val queueManager = QueueManager()

    fun initialize() {
        Log.info("XDM KMP v8.0.0 initializing...")
        Log.info("Platform: ${PlatformInfo.os}, Arch: ${PlatformInfo.arch}")
        Log.info("Config dir: $configDir")

        configManager.load()

        if (configManager.config.isBrowserMonitorEnabled) {
            browserMonitor.start()
        }

        queueManager.startScheduler { queue ->
            Log.info("Queue ${queue.name} ready, processing downloads")
        }

        Log.info("XDM initialized")
    }

    fun shutdown() {
        Log.info("XDM shutting down...")
        browserMonitor.stop()
        queueManager.stopScheduler()
        configManager.save()
        Log.info("XDM shutdown complete")
    }
}
