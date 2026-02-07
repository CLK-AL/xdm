package xdm.util

import java.io.File

internal actual fun platformLog(level: String, msg: String) {
    println("[$level] $msg")
}

actual class FileSystem actual constructor() {
    actual fun exists(path: String): Boolean = File(path).exists()
    actual fun readText(path: String): String = File(path).readText()
    actual fun writeText(path: String, content: String) = File(path).writeText(content)
    actual fun readBytes(path: String): ByteArray = File(path).readBytes()
    actual fun writeBytes(path: String, data: ByteArray) = File(path).writeBytes(data)
    actual fun ensureDirectoryExists(path: String) { File(path).mkdirs() }
    actual fun delete(path: String) { File(path).delete() }
    actual fun move(src: String, dst: String) { File(src).renameTo(File(dst)) }
    actual fun listFiles(dir: String): List<String> =
        File(dir).listFiles()?.map { it.absolutePath } ?: emptyList()
    actual fun fileSize(path: String): Long = File(path).length()
    actual fun tempDir(): String = System.getProperty("java.io.tmpdir")
    actual fun homeDir(): String = System.getProperty("user.home")
    actual fun pathSeparator(): String = File.separator
    actual fun combinePath(vararg parts: String): String =
        parts.fold(File("")) { acc, part -> File(acc, part) }.path
}

actual object PlatformInfo {
    actual val os: OsType
        get() {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                "windows" in osName -> OsType.Windows
                "mac" in osName || "darwin" in osName -> OsType.MacOS
                "linux" in osName || "unix" in osName -> OsType.Linux
                else -> OsType.Unknown
            }
        }

    actual val arch: String
        get() = System.getProperty("os.arch")

    actual val appDataDir: String
        get() = when (os) {
            OsType.Windows -> System.getenv("APPDATA") ?: "${homeDir()}\\AppData\\Roaming"
            OsType.MacOS -> "${homeDir()}/Library/Application Support"
            else -> "${homeDir()}/.config"
        }

    private fun homeDir() = System.getProperty("user.home")
}
