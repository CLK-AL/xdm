package xdm.queue

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import xdm.util.Helpers
import xdm.util.Log

/**
 * Download queue management with scheduling.
 *
 * Comparison:
 * - Java: QueueManager + QueueScheduler. Queues stored in ArrayList, timer-based scheduling.
 *         QueueScheduler runs on a separate Thread, checks time windows.
 * - C#:   QueueManager with scheduled start/end times. Timer-based scheduling.
 *         Queues are serialized via custom binary format.
 * - KMP:  Coroutine-based scheduling using delay(). Queues serialized as JSON.
 *         Structured concurrency for queue processing lifecycle.
 */

@Serializable
data class DownloadQueue(
    val id: String = Helpers.generateId(),
    val name: String = "Default",
    val downloadIds: MutableList<String> = mutableListOf(),
    val schedule: QueueSchedule? = null,
    val isRunning: Boolean = false,
)

@Serializable
data class QueueSchedule(
    val enabled: Boolean = false,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 23,
    val endMinute: Int = 59,
    val daysOfWeek: Set<Int> = setOf(0, 1, 2, 3, 4, 5, 6), // 0=Sunday
)

class QueueManager {
    private val queues = mutableMapOf<String, DownloadQueue>()
    private var schedulerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun createQueue(name: String): DownloadQueue {
        val queue = DownloadQueue(name = name)
        queues[queue.id] = queue
        return queue
    }

    fun deleteQueue(id: String) {
        queues.remove(id)
    }

    fun addToQueue(queueId: String, downloadId: String) {
        queues[queueId]?.downloadIds?.add(downloadId)
    }

    fun removeFromQueue(queueId: String, downloadId: String) {
        queues[queueId]?.downloadIds?.remove(downloadId)
    }

    fun getQueue(id: String): DownloadQueue? = queues[id]

    fun getAllQueues(): List<DownloadQueue> = queues.values.toList()

    fun startScheduler(onQueueReady: suspend (DownloadQueue) -> Unit) {
        schedulerJob?.cancel()
        schedulerJob = scope.launch {
            while (isActive) {
                for (queue in queues.values) {
                    val schedule = queue.schedule
                    if (schedule != null && schedule.enabled && isWithinSchedule(schedule)) {
                        if (!queue.isRunning && queue.downloadIds.isNotEmpty()) {
                            onQueueReady(queue)
                        }
                    }
                }
                delay(60_000) // Check every minute
            }
        }
    }

    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    private fun isWithinSchedule(schedule: QueueSchedule): Boolean {
        // Simplified check - would use kotlinx-datetime for proper implementation
        return schedule.enabled
    }
}
