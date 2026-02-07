package xdm.config

import kotlinx.serialization.json.Json
import xdm.util.FileSystem
import xdm.util.Log

/**
 * Manages application configuration with persistence.
 * Thread-safe via atomic reference update pattern.
 *
 * Unlike Java/C# versions that used custom binary serialization,
 * KMP uses JSON for cross-platform compatibility and debuggability.
 */
class ConfigManager(
    private val fs: FileSystem,
    private val configDir: String,
) {
    @Volatile
    private var _config: AppConfig = AppConfig()

    val config: AppConfig get() = _config

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load() {
        try {
            val path = "$configDir/config.json"
            if (fs.exists(path)) {
                val content = fs.readText(path)
                _config = json.decodeFromString<AppConfig>(content)
                Log.info("Config loaded from $path")
            } else {
                Log.info("No config found, using defaults")
            }
        } catch (e: Exception) {
            Log.error("Failed to load config: ${e.message}")
        }
    }

    fun save() {
        try {
            val path = "$configDir/config.json"
            fs.ensureDirectoryExists(configDir)
            val content = json.encodeToString(AppConfig.serializer(), _config)
            fs.writeText(path, content)
            Log.info("Config saved to $path")
        } catch (e: Exception) {
            Log.error("Failed to save config: ${e.message}")
        }
    }

    fun update(transform: (AppConfig) -> AppConfig) {
        _config = transform(_config)
        save()
    }
}
