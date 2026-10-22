package ch.ebu.peachcollector

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Instrumented tests for PeachPlayerTracker.
 * Drives a real ExoPlayer playing a generated silent WAV file and checks the events stored by the tracker.
 */
@RunWith(AndroidJUnit4::class)
class PeachPlayerTrackerInstrumentedTest {

    companion object {
        const val PUBLISHER_NAME = "DefaultPublisher"
        const val MEDIA_ID = "media001"
        const val MEDIA_DURATION_S = 10
        const val TIMEOUT_MS = 10_000L
        const val SETTLE_MS = 1_000L
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var context: Context
    private lateinit var publisher: Publisher
    private lateinit var player: ExoPlayer

    @Before
    fun initializeModule() {
        context = instrumentation.targetContext

        PeachCollector.isUnitTesting = true
        PeachCollector.shouldCollectAnonymousEvents = true

        val app = context.applicationContext as Application
        PeachCollector.init(app)
        PeachCollector.clean()

        // No service URL: events are stored but never published, so they can be read back
        publisher = Publisher()
        PeachCollector.addPublisher(publisher, PUBLISHER_NAME)

        val mediaFile = createSilentWav(File(context.cacheDir, "silence.wav"), MEDIA_DURATION_S)
        onMain {
            player = ExoPlayer.Builder(context).build()
            // Logs the player callbacks to logcat (tag EventLogger) to help understand failures
            player.addAnalyticsListener(EventLogger())
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(mediaFile)))
            PeachPlayerTracker.setPlayer(player)
        }
    }

    @After
    fun tearDown() {
        onMain {
            PeachPlayerTracker.clearCurrentItem()
            player.release()
        }
        PeachCollector.clean()
    }

    @Test
    fun testPlayBeforeReady() {
        onMain {
            PeachPlayerTracker.trackMedia(MEDIA_ID)
            player.prepare()
            player.play()
        }
        waitUntil("playback to start") { player.isPlaying }

        assertEquals(listOf(EventType.MEDIA_PLAY), trackedEvents())
    }

    @Test
    fun testPlayAfterReady() {
        startPlaybackAfterReady()

        assertEquals(listOf(EventType.MEDIA_PLAY), trackedEvents())
    }

    @Test
    fun testPauseAndResume() {
        startPlaybackAfterReady()
        onMain { player.pause() }
        assertEquals(listOf(EventType.MEDIA_PLAY, EventType.MEDIA_PAUSE), trackedEvents())

        onMain { player.play() }
        waitUntil("playback to resume") { player.isPlaying }

        assertEquals(
            listOf(EventType.MEDIA_PLAY, EventType.MEDIA_PAUSE, EventType.MEDIA_PLAY),
            trackedEvents()
        )
    }

    @Test
    fun testSeekWhilePlaying() {
        startPlaybackAfterReady()
        assertEquals(listOf(EventType.MEDIA_PLAY), trackedEvents())

        onMain { player.seekTo(5_000) }
        waitUntil("playback to resume after the seek") {
            player.playbackState == Player.STATE_READY && player.isPlaying
        }

        assertEquals(listOf(EventType.MEDIA_PLAY, EventType.MEDIA_SEEK), trackedEvents())
    }

    @Test
    fun testTrackMediaWhilePlaying() {
        onMain {
            player.prepare()
            player.play()
        }
        waitUntil("playback to start") { player.isPlaying }
        onMain { PeachPlayerTracker.trackMedia(MEDIA_ID) }

        assertEquals(listOf(EventType.MEDIA_PLAY), trackedEvents())
    }

    @Test
    fun testReplayAfterEnd() {
        startPlaybackAfterReady()
        assertEquals(listOf(EventType.MEDIA_PLAY), trackedEvents())

        onMain { player.seekTo((MEDIA_DURATION_S - 1) * 1000L) }
        waitUntil("playback to end") { player.playbackState == Player.STATE_ENDED }
        assertEquals(
            listOf(EventType.MEDIA_PLAY, EventType.MEDIA_SEEK, EventType.MEDIA_END),
            trackedEvents()
        )

        // playWhenReady is still true after the end, so seeking back restarts playback
        onMain { player.seekTo(0) }
        waitUntil("playback to restart") { player.isPlaying }

        assertEquals(
            listOf(EventType.MEDIA_PLAY, EventType.MEDIA_SEEK, EventType.MEDIA_END, EventType.MEDIA_SEEK, EventType.MEDIA_PLAY),
            trackedEvents()
        )
    }

    @Test
    fun testPlayAfterStop() {
        startPlaybackAfterReady()
        onMain { player.stop() }
        assertEquals(listOf(EventType.MEDIA_PLAY), trackedEvents())

        // playWhenReady is still true after stop, so preparing again restarts playback
        onMain { player.prepare() }
        waitUntil("playback to restart") { player.isPlaying }

        assertEquals(listOf(EventType.MEDIA_PLAY, EventType.MEDIA_PLAY), trackedEvents())
    }

    @Test
    fun testHeartbeatsOnlyWhilePlaying() {
        publisher.playerTrackerHeartbeatInterval = 1
        startPlaybackAfterReady()
        Thread.sleep(2_500)
        onMain { player.pause() }

        val heartbeats = storedEvents().count { it == EventType.MEDIA_HEARTBEAT }
        assertTrue("No heartbeat was sent while playing", heartbeats > 0)

        Thread.sleep(2_000)
        assertEquals(
            "A heartbeat was sent while paused",
            heartbeats,
            storedEvents().count { it == EventType.MEDIA_HEARTBEAT }
        )
    }

    // region Helpers

    /** Starts tracking, then starts playback once the player is ready. */
    private fun startPlaybackAfterReady() {
        onMain {
            PeachPlayerTracker.trackMedia(MEDIA_ID)
            player.prepare()
        }
        waitUntil("the player to be ready") { player.playbackState == Player.STATE_READY }
        onMain { player.play() }
        waitUntil("playback to start") { player.isPlaying }
    }

    /** The player and the tracker must be used from the main thread. */
    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private fun waitUntil(description: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!onMain(condition)) {
            if (System.currentTimeMillis() > deadline) fail("Timed out waiting for $description")
            Thread.sleep(20)
        }
    }

    /** Types of the stored events, in the order they were queued. Waits for pending inserts first. */
    private fun storedEvents(): List<String> {
        Thread.sleep(SETTLE_MS)
        return runBlocking { PeachCollector.shared!!.database.eventDao().getAllEvents() }
            .sortedBy { it.id }
            .map { it.type }
    }

    /** Types of the stored events, without heartbeats. */
    private fun trackedEvents(): List<String> =
        storedEvents().filter { it != EventType.MEDIA_HEARTBEAT }

    /** Writes a 16-bit mono PCM WAV file containing silence. */
    private fun createSilentWav(file: File, durationSeconds: Int): File {
        val sampleRate = 44_100
        val dataSize = sampleRate * 2 * durationSeconds
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)              // fmt chunk size
            putShort(1)             // PCM
            putShort(1)             // mono
            putInt(sampleRate)
            putInt(sampleRate * 2)  // byte rate
            putShort(2)             // block align
            putShort(16)            // bits per sample
            put("data".toByteArray())
            putInt(dataSize)
        }
        file.outputStream().use {
            it.write(header.array())
            it.write(ByteArray(dataSize))
        }
        return file
    }

    // endregion
}
