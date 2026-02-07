package xdm.util

/**
 * Platform abstraction layer for file system, logging, and OS operations.
 * expect/actual pattern allows each platform to provide native implementations.
 */

// -- Logging --

object Log {
    var enabled: Boolean = true
    var debugEnabled: Boolean = false

    fun info(msg: String) {
        if (enabled) platformLog("INFO", msg)
    }

    fun error(msg: String, throwable: Throwable? = null) {
        if (enabled) {
            platformLog("ERROR", msg)
            throwable?.let { platformLog("ERROR", it.stackTraceToString()) }
        }
    }

    fun debug(msg: String) {
        if (debugEnabled) platformLog("DEBUG", msg)
    }
}

internal expect fun platformLog(level: String, msg: String)

// -- File system --

expect class FileSystem() {
    fun exists(path: String): Boolean
    fun readText(path: String): String
    fun writeText(path: String, content: String)
    fun readBytes(path: String): ByteArray
    fun writeBytes(path: String, data: ByteArray)
    fun ensureDirectoryExists(path: String)
    fun delete(path: String)
    fun move(src: String, dst: String)
    fun listFiles(dir: String): List<String>
    fun fileSize(path: String): Long
    fun tempDir(): String
    fun homeDir(): String
    fun pathSeparator(): String
    fun combinePath(vararg parts: String): String
}

// -- Platform info --

expect object PlatformInfo {
    val os: OsType
    val arch: String
    val appDataDir: String
}

enum class OsType {
    Windows,
    Linux,
    MacOS,
    Ios,
    Unknown,
}
