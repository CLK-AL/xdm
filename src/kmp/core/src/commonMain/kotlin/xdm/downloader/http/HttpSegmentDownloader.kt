package xdm.downloader.http

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import xdm.downloader.ProbeResult
import xdm.downloader.SegmentDownloader
import xdm.model.DownloadMetadata
import xdm.model.HttpDownloadMetadata
import xdm.model.Segment
import xdm.util.FileSystem
import xdm.util.Helpers
import xdm.util.Log

/**
 * HTTP segment downloader using Ktor.
 *
 * Comparison:
 * - Java: HttpChannel extends AbstractChannel, uses raw Socket/SSLSocket for HTTP.
 *         Custom HTTP parser, chunked encoding handler, keep-alive management.
 *         Also has JavaHttpClient as fallback using java.net.HttpURLConnection.
 * - C#:   DotNetHttpClient wraps System.Net.Http.HttpClient.
 *         WinHttpClient wraps Windows WinHTTP API via P/Invoke.
 *         Both disable TLS certificate validation entirely.
 * - KMP:  Uses Ktor HttpClient which provides platform-native TLS by default.
 *         Proper certificate validation. Automatic engine selection per platform
 *         (CIO for JVM, Curl for native, Darwin for iOS).
 */
class HttpSegmentDownloader(
    private val client: HttpClient,
    private val fs: FileSystem,
) : SegmentDownloader {

    override suspend fun probe(metadata: DownloadMetadata): ProbeResult {
        val httpMeta = metadata as HttpDownloadMetadata

        val response = client.head(httpMeta.url) {
            applyHeaders(httpMeta)
        }

        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        val acceptRanges = response.headers[HttpHeaders.AcceptRanges]?.contains("bytes") == true
        val contentDisposition = response.headers[HttpHeaders.ContentDisposition]
        val contentType = response.headers[HttpHeaders.ContentType]

        val fileName = parseContentDispositionFileName(contentDisposition)
            ?: Helpers.getFileNameFromUrl(httpMeta.url)

        return ProbeResult(
            totalSize = contentLength,
            resumable = acceptRanges && contentLength > 0,
            fileName = fileName,
            contentType = contentType,
            redirectedUrl = response.request.url.toString()
                .takeIf { it != httpMeta.url },
        )
    }

    override suspend fun downloadSegment(
        metadata: DownloadMetadata,
        segment: Segment,
        outputPath: String,
        onProgress: (bytesDownloaded: Long) -> Unit,
    ) {
        val httpMeta = metadata as HttpDownloadMetadata
        var downloaded = segment.downloaded

        val response = client.prepareGet(httpMeta.url) {
            applyHeaders(httpMeta)
            if (segment.startOffset > 0 || segment.length > 0) {
                val rangeEnd = if (segment.length > 0) {
                    segment.startOffset + segment.length - 1
                } else ""
                header(HttpHeaders.Range, "bytes=${segment.currentOffset}-$rangeEnd")
            }
        }

        response.execute { httpResponse ->
            val channel = httpResponse.bodyAsChannel()
            val buffer = ByteArray(8192)

            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                if (bytesRead <= 0) break

                // Write to file (append mode for resumed segments)
                fs.appendBytes(outputPath, buffer.copyOf(bytesRead))

                downloaded += bytesRead
                onProgress(downloaded)
            }
        }
    }

    private fun HttpRequestBuilder.applyHeaders(meta: HttpDownloadMetadata) {
        for ((key, values) in meta.headers) {
            for (value in values) {
                header(key, value)
            }
        }
        for ((name, value) in meta.cookies) {
            cookie(name, value)
        }
    }

    private fun parseContentDispositionFileName(header: String?): String? {
        if (header == null) return null
        // Parse: attachment; filename="example.zip" or filename*=UTF-8''example.zip
        val filenameRegex = Regex("""filename\*?=(?:UTF-8''|"?)([^";]+)"?""", RegexOption.IGNORE_CASE)
        return filenameRegex.find(header)?.groupValues?.get(1)
            ?.let { Helpers.sanitizeFileName(it) }
    }
}

/**
 * Extension: append bytes to a file (platform-specific implementations expected).
 */
expect fun FileSystem.appendBytes(path: String, data: ByteArray)
