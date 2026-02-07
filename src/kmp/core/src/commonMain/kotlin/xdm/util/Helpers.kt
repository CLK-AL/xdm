package xdm.util

import kotlinx.datetime.Clock

/**
 * General utility functions.
 * Consolidated from Java XDMUtils/StringUtils/FormatUtilities
 * and C# Helpers/FileHelper.
 */
object Helpers {

    fun tickCount(): Long = Clock.System.now().toEpochMilliseconds()

    fun generateId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString(8) {
            repeat(8) { append(chars.random()) }
        }
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "Unknown"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }

    fun formatSpeed(bytesPerSecond: Long): String = when {
        bytesPerSecond <= 0 -> "0 B/s"
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSecond / 1024.0)
        else -> "%.1f MB/s".format(bytesPerSecond / (1024.0 * 1024))
    }

    fun formatEta(seconds: Long): String = when {
        seconds < 0 -> "--"
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }

    /**
     * Sanitize a file name by removing path separators and invalid characters.
     * Fixes the incomplete sanitization from both Java and C# versions.
     */
    fun sanitizeFileName(fileName: String): String {
        if (fileName.isBlank()) return "download"

        // Strip any path components
        val name = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')

        // Replace invalid characters
        val invalidChars = charArrayOf(
            '<', '>', ':', '"', '|', '?', '*',
            '\u0000', '\n', '\r', '\t',
        )
        var sanitized = name
        for (c in invalidChars) {
            sanitized = sanitized.replace(c, '_')
        }

        // Handle Windows reserved device names
        val reservedNames = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        )
        val baseName = sanitized.substringBefore('.').uppercase()
        if (baseName in reservedNames) {
            sanitized = "_$sanitized"
        }

        return sanitized.ifBlank { "download" }
    }

    fun getFileExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot >= 0 && dot < fileName.length - 1) {
            fileName.substring(dot + 1)
        } else ""
    }

    fun getFileNameWithoutExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > 0) fileName.substring(0, dot) else fileName
    }

    fun getFileNameFromUrl(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val name = path.substringAfterLast('/')
        return sanitizeFileName(name.ifBlank { "download" })
    }

    fun categorizeFile(fileName: String): xdm.model.FileCategory {
        val ext = getFileExtension(fileName).uppercase()
        return when (ext) {
            in setOf("ZIP", "RAR", "7Z", "GZ", "BZ2", "XZ", "TAR", "ISO", "IMG") ->
                xdm.model.FileCategory.Compressed
            in setOf("PDF", "DOC", "DOCX", "XLS", "XLSX", "PPT", "PPTX", "TXT", "ODT") ->
                xdm.model.FileCategory.Documents
            in setOf("MP3", "FLAC", "AAC", "WAV", "WMA", "OGG", "M4A", "OPUS") ->
                xdm.model.FileCategory.Music
            in setOf("MP4", "MKV", "WEBM", "FLV", "MOV", "AVI", "WMV", "3GP", "M4V") ->
                xdm.model.FileCategory.Video
            in setOf("EXE", "MSI", "DEB", "RPM", "DMG", "APK", "JAR", "RUN", "SH") ->
                xdm.model.FileCategory.Programs
            else -> xdm.model.FileCategory.Other
        }
    }
}

/**
 * Simple string formatting for multiplatform.
 */
fun String.format(vararg args: Any?): String {
    var result = this
    var argIndex = 0
    val regex = Regex("""%(\.\d+)?[dfse]""")
    return regex.replace(result) { match ->
        if (argIndex < args.size) {
            val arg = args[argIndex++]
            when {
                match.value.endsWith("d") -> (arg as? Number)?.toLong()?.toString() ?: arg.toString()
                match.value.endsWith("f") -> {
                    val decimals = match.groupValues[1].removePrefix(".").toIntOrNull() ?: 6
                    val num = (arg as? Number)?.toDouble() ?: 0.0
                    formatDouble(num, decimals)
                }
                else -> arg.toString()
            }
        } else match.value
    }
}

private fun formatDouble(value: Double, decimals: Int): String {
    val factor = Math.pow(10.0, decimals.toDouble())
    val rounded = Math.round(value * factor) / factor
    val str = rounded.toString()
    val dot = str.indexOf('.')
    return if (dot < 0) {
        "$str.${"0".repeat(decimals)}"
    } else {
        val currentDecimals = str.length - dot - 1
        if (currentDecimals >= decimals) str.substring(0, dot + decimals + 1)
        else str + "0".repeat(decimals - currentDecimals)
    }
}

private object Math {
    fun pow(base: Double, exp: Double): Double {
        var result = 1.0
        repeat(exp.toInt()) { result *= base }
        return result
    }

    fun round(value: Double): Long = (value + 0.5).toLong()
}
