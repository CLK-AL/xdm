package xdm.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

actual class ProcessRunner actual constructor() {
    actual suspend fun run(
        executable: String,
        args: List<String>,
        onOutput: (String) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val pb = ProcessBuilder(listOf(executable) + args)
        pb.redirectErrorStream(true)

        val process = pb.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            onOutput(line!!)
        }

        process.waitFor()
    }
}
