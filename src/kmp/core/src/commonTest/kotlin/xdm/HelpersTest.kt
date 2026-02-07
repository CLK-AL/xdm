package xdm

import kotlin.test.Test
import kotlin.test.assertEquals
import xdm.util.Helpers
import xdm.model.FileCategory

class HelpersTest {

    @Test
    fun sanitizeFileName_removesPathComponents() {
        assertEquals("file.txt", Helpers.sanitizeFileName("/etc/passwd/file.txt"))
        assertEquals("file.txt", Helpers.sanitizeFileName("C:\\Users\\file.txt"))
    }

    @Test
    fun sanitizeFileName_replacesInvalidChars() {
        assertEquals("file_name_.txt", Helpers.sanitizeFileName("file<name>.txt"))
        assertEquals("file_txt", Helpers.sanitizeFileName("file:txt"))
    }

    @Test
    fun sanitizeFileName_handlesReservedNames() {
        assertEquals("_CON.txt", Helpers.sanitizeFileName("CON.txt"))
        assertEquals("_NUL", Helpers.sanitizeFileName("NUL"))
        assertEquals("_COM1.log", Helpers.sanitizeFileName("COM1.log"))
    }

    @Test
    fun sanitizeFileName_handlesBlank() {
        assertEquals("download", Helpers.sanitizeFileName(""))
        assertEquals("download", Helpers.sanitizeFileName("   "))
    }

    @Test
    fun getFileNameFromUrl_basic() {
        assertEquals("file.zip", Helpers.getFileNameFromUrl("https://example.com/files/file.zip"))
        assertEquals("file.zip", Helpers.getFileNameFromUrl("https://example.com/files/file.zip?token=abc"))
    }

    @Test
    fun categorizeFile_correctCategories() {
        assertEquals(FileCategory.Video, Helpers.categorizeFile("movie.mp4"))
        assertEquals(FileCategory.Music, Helpers.categorizeFile("song.mp3"))
        assertEquals(FileCategory.Compressed, Helpers.categorizeFile("archive.zip"))
        assertEquals(FileCategory.Documents, Helpers.categorizeFile("readme.pdf"))
        assertEquals(FileCategory.Programs, Helpers.categorizeFile("setup.exe"))
        assertEquals(FileCategory.Other, Helpers.categorizeFile("data.csv"))
    }

    @Test
    fun formatSize_humanReadable() {
        assertEquals("0 B", Helpers.formatSize(0))
        assertEquals("512 B", Helpers.formatSize(512))
        assertEquals("1.0 KB", Helpers.formatSize(1024))
        assertEquals("1.5 MB", Helpers.formatSize(1572864))
    }

    @Test
    fun formatEta_humanReadable() {
        assertEquals("--", Helpers.formatEta(-1))
        assertEquals("30s", Helpers.formatEta(30))
        assertEquals("5m 30s", Helpers.formatEta(330))
        assertEquals("1h 30m", Helpers.formatEta(5400))
    }

    @Test
    fun getFileExtension_works() {
        assertEquals("txt", Helpers.getFileExtension("file.txt"))
        assertEquals("gz", Helpers.getFileExtension("archive.tar.gz"))
        assertEquals("", Helpers.getFileExtension("noext"))
    }
}
