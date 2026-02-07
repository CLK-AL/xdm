package xdm.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xdm.util.Log

/**
 * yt-dlp integration for video URL extraction.
 *
 * Conversion from Java YoutubeDLHandler.java:
 * - Java: Runs yt-dlp process, parses JSON output for video formats.
 *         Arguments built via string concatenation (injection risk).
 * - C#:   YDLProcess.cs - same approach, same injection vulnerability.
 *         URI/username/password interpolated directly into command string.
 * - KMP:  Uses argument list (no shell interpolation). Process launched
 *         via ProcessRunner expect/actual. Input sanitized.
 */

@Serializable
data class YtDlpResult(
    val title: String = "",
    val formats: List<YtDlpFormat> = emptyList(),
    val thumbnail: String = "",
    val duration: Double = 0.0,
)

@Serializable
data class YtDlpFormat(
    val format_id: String = "",
    val ext: String = "",
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val filesize: Long? = null,
    val vcodec: String = "none",
    val acodec: String = "none",
    val tbr: Double? = null,
    val format_note: String = "",
) {
    val hasVideo: Boolean get() = vcodec != "none"
    val hasAudio: Boolean get() = acodec != "none"
    val resolution: String get() = if (width != null && height != null) "${width}x$height" else ""
}

class YtDlpWrapper(
    private val ytDlpPath: String,
    private val processRunner: ProcessRunner,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Extract video information from a URL.
     * Arguments are passed as a list to avoid shell injection.
     */
    suspend fun extractInfo(
        url: String,
        browserName: String? = null,
        userName: String? = null,
        password: String? = null,
    ): YtDlpResult? {
        val args = buildList {
            add("--no-warnings")
            add("-q")
            add("-i")
            add("-J") // JSON output

            if (!browserName.isNullOrBlank()) {
                add("--cookies-from-browser")
                add(browserName) // Separate argument, no interpolation
            }

            if (!userName.isNullOrBlank()) {
                add("--username")
                add(userName)
                if (!password.isNullOrBlank()) {
                    add("--password")
                    add(password)
                }
            }

            add("--") // End of options, URL follows
            add(url)
        }

        val output = StringBuilder()
        val exitCode = processRunner.run(ytDlpPath, args) { line ->
            output.appendLine(line)
        }

        if (exitCode != 0) {
            Log.error("yt-dlp exited with code $exitCode")
            return null
        }

        return try {
            json.decodeFromString<YtDlpResult>(output.toString())
        } catch (e: Exception) {
            Log.error("Failed to parse yt-dlp output", e)
            null
        }
    }

    suspend fun isAvailable(): Boolean {
        return try {
            var found = false
            processRunner.run(ytDlpPath, listOf("--version")) { line ->
                if (line.isNotBlank()) found = true
            }
            found
        } catch (e: Exception) {
            false
        }
    }
}
