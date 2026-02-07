package xdm.platform

import xdm.downloader.http.FileSystem
import xdm.downloader.http.appendBytes
import java.io.File
import java.io.FileOutputStream

/**
 * JVM implementation of file append for segment downloads.
 */
actual fun xdm.util.FileSystem.appendBytes(path: String, data: ByteArray) {
    FileOutputStream(File(path), true).use { it.write(data) }
}
