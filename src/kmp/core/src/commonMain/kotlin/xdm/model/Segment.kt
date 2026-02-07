package xdm.model

import kotlinx.serialization.Serializable

/**
 * Represents a download segment/piece/chunk.
 * Unified from Java Segment interface and C# Piece class.
 *
 * Java used: Segment (interface) -> SegmentImpl
 * C# used: Piece (class with offset/length/downloaded/state)
 */
@Serializable
data class Segment(
    val id: String,
    var startOffset: Long = 0L,
    var length: Long = -1L,
    var downloaded: Long = 0L,
    var state: SegmentState = SegmentState.NotStarted,
    var errorCode: Int = 0,
    var tag: String? = null,
) {
    val isFinished: Boolean get() = state == SegmentState.Finished
    val isActive: Boolean get() = state == SegmentState.Downloading

    val remainingBytes: Long
        get() = if (length < 0) -1L else length - downloaded

    val currentOffset: Long
        get() = startOffset + downloaded
}

@Serializable
enum class SegmentState {
    NotStarted,
    Downloading,
    Finished,
    Failed,
    Stopped,
}
