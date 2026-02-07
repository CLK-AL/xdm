package xdm.media

import xdm.util.Log

/**
 * FFmpeg integration for media conversion.
 *
 * Conversion from Java FFmpeg.java:
 * - Java: Launches ffmpeg process with ProcessBuilder, parses progress from stderr.
 *         Uses custom format database loaded from resources.
 * - C#:   FFmpegMediaProcessor wraps ffmpeg similarly. Uses ArgumentList.Add() on .NET 5+
 *         for safe argument passing (unlike YDLProcess which uses string concatenation).
 * - KMP:  Platform expect/actual for process launching. Argument list built safely.
 */

data class ConversionFormat(
    val name: String,
    val extension: String,
    val videoCodec: String = "",
    val audioCodec: String = "",
    val extraArgs: List<String> = emptyList(),
)

data class ConversionProgress(
    val percent: Int,
    val timeProcessed: String = "",
    val speed: String = "",
)

expect class ProcessRunner() {
    suspend fun run(
        executable: String,
        args: List<String>,
        onOutput: (String) -> Unit,
    ): Int
}

class FFmpegRunner(
    private val ffmpegPath: String,
    private val processRunner: ProcessRunner,
) {
    suspend fun convert(
        inputPath: String,
        outputPath: String,
        format: ConversionFormat,
        onProgress: (ConversionProgress) -> Unit,
    ): Boolean {
        val args = buildList {
            add("-y") // Overwrite
            add("-i")
            add(inputPath)
            if (format.videoCodec.isNotBlank()) {
                add("-c:v")
                add(format.videoCodec)
            }
            if (format.audioCodec.isNotBlank()) {
                add("-c:a")
                add(format.audioCodec)
            }
            addAll(format.extraArgs)
            add("-progress")
            add("pipe:1")
            add(outputPath)
        }

        Log.info("Running ffmpeg: $ffmpegPath ${args.joinToString(" ")}")

        val exitCode = processRunner.run(ffmpegPath, args) { line ->
            parseProgress(line)?.let { onProgress(it) }
        }

        return exitCode == 0
    }

    suspend fun mergeAudioVideo(
        videoPath: String,
        audioPath: String,
        outputPath: String,
        onProgress: (ConversionProgress) -> Unit,
    ): Boolean {
        val args = listOf(
            "-y",
            "-i", videoPath,
            "-i", audioPath,
            "-c:v", "copy",
            "-c:a", "copy",
            "-progress", "pipe:1",
            outputPath,
        )

        val exitCode = processRunner.run(ffmpegPath, args) { line ->
            parseProgress(line)?.let { onProgress(it) }
        }

        return exitCode == 0
    }

    private fun parseProgress(line: String): ConversionProgress? {
        // ffmpeg progress output: "out_time=00:01:23.456" or "progress=continue/end"
        if (line.startsWith("out_time=")) {
            val time = line.removePrefix("out_time=").trim()
            return ConversionProgress(percent = -1, timeProcessed = time)
        }
        if (line.startsWith("speed=")) {
            val speed = line.removePrefix("speed=").trim()
            return ConversionProgress(percent = -1, speed = speed)
        }
        return null
    }

    companion object {
        val DEFAULT_FORMATS = listOf(
            ConversionFormat("MP4 (H.264/AAC)", "mp4", "libx264", "aac"),
            ConversionFormat("MKV (H.264/AAC)", "mkv", "libx264", "aac"),
            ConversionFormat("WEBM (VP9/Opus)", "webm", "libvpx-vp9", "libopus"),
            ConversionFormat("MP3", "mp3", "", "libmp3lame"),
            ConversionFormat("AAC", "m4a", "", "aac"),
            ConversionFormat("FLAC", "flac", "", "flac"),
            ConversionFormat("OGG (Vorbis)", "ogg", "", "libvorbis"),
            ConversionFormat("WAV", "wav", "", "pcm_s16le"),
        )
    }
}
