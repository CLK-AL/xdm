package xdm

import kotlin.test.Test
import kotlin.test.assertEquals
import xdm.downloader.Download
import xdm.model.Segment
import xdm.model.SegmentState

class DownloadEngineTest {

    @Test
    fun createSegments_singleSegmentForUnknownSize() {
        val segments = Download.createSegments(totalSize = -1, resumable = false, maxSegments = 8)
        assertEquals(1, segments.size)
        assertEquals(0L, segments[0].startOffset)
    }

    @Test
    fun createSegments_singleSegmentWhenNotResumable() {
        val segments = Download.createSegments(totalSize = 1000, resumable = false, maxSegments = 8)
        assertEquals(1, segments.size)
        assertEquals(1000L, segments[0].length)
    }

    @Test
    fun createSegments_multipleSegmentsWhenResumable() {
        val segments = Download.createSegments(totalSize = 1000, resumable = true, maxSegments = 4)
        assertEquals(4, segments.size)
        assertEquals(0L, segments[0].startOffset)
        assertEquals(250L, segments[0].length)
        assertEquals(250L, segments[1].startOffset)
        assertEquals(250L, segments[1].length)
        assertEquals(500L, segments[2].startOffset)
        assertEquals(250L, segments[2].length)
        assertEquals(750L, segments[3].startOffset)
        assertEquals(250L, segments[3].length)
    }

    @Test
    fun createSegments_lastSegmentGetsRemainder() {
        val segments = Download.createSegments(totalSize = 1003, resumable = true, maxSegments = 4)
        assertEquals(4, segments.size)
        // 1003 / 4 = 250 per segment, last gets 253
        assertEquals(253L, segments[3].length)
    }

    @Test
    fun segment_currentOffset() {
        val segment = Segment(id = "s0", startOffset = 100, length = 500, downloaded = 200)
        assertEquals(300L, segment.currentOffset)
        assertEquals(300L, segment.remainingBytes)
    }

    @Test
    fun segment_states() {
        val segment = Segment(id = "s0")
        assertEquals(false, segment.isFinished)
        assertEquals(false, segment.isActive)

        val active = segment.copy(state = SegmentState.Downloading)
        assertEquals(true, active.isActive)

        val finished = segment.copy(state = SegmentState.Finished)
        assertEquals(true, finished.isFinished)
    }
}
