package ch.ebu.peachcollector

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlinx.coroutines.*
import java.util.Date

/**
 * Tracks Media3 ExoPlayer state and automatically sends playback events.
 * Manages heartbeat timers per publisher for continuous playback tracking.
 *
 * Mirrors the Java PeachPlayerTracker behavior exactly.
 */
object PeachPlayerTracker {

    private var player: Player? = null
    private var itemID: String? = null
    private var props: EventProperties? = null
    private var context: EventContext? = null
    private var metadata: Map<String, Any>? = null

    private val heartbeatJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var trackingStartDate: Date? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (itemID == null) return

            updateTimeSpent()
            if (playbackState == Player.STATE_READY && player?.isPlaying == true) {
                // Playing
                startHeartbeats()
                props?.playbackPosition = (player?.currentPosition?.div(1000))?.toDouble()
                Event.sendMediaPlay(itemID!!, props, context, metadata)
            } else if (playbackState == Player.STATE_ENDED) {
                // Reached the end
                stopHeartbeats()
                props?.playbackPosition = (player?.currentPosition?.div(1000))?.toDouble()
                Event.sendMediaEnd(itemID!!, props, context, metadata)
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (itemID == null) return

            updateTimeSpent()
            if (playWhenReady) {
                // Playing
                startHeartbeats()
                props?.playbackPosition = (player?.currentPosition?.div(1000))?.toDouble()
                Event.sendMediaPlay(itemID!!, props, context, metadata)
            } else {
                // Paused
                stopHeartbeats()
                props?.playbackPosition = (player?.currentPosition?.div(1000))?.toDouble()
                Event.sendMediaPause(itemID!!, props, context, metadata)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            @Player.DiscontinuityReason reason: Int
        ) {
            if (itemID == null) return

            val newPos = (newPosition.positionMs / 1000).toDouble()
            val oldPos = (oldPosition.positionMs / 1000).toDouble()
            if (newPos == oldPos) return

            updateTimeSpent()
            props?.playbackPosition = newPos
            props?.previousPlaybackPosition = oldPos
            Event.sendMediaSeek(itemID!!, props, context, metadata)
            props?.previousPlaybackPosition = null
        }

        override fun onVolumeChanged(volume: Float) {
            props?.volume = volume
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            props?.playbackRate = playbackParameters.speed
        }
    }

    /**
     * Sets the Media3 Player instance to track.
     * Listener is added when trackMedia is called.
     */
    fun setPlayer(player: Player) {
        this.player = player

        if (itemID != null) {
            player.addListener(playerListener)
        }
    }

    /**
     * Starts tracking a media item.
     *
     * @param mediaID Unique identifier for the media item
     * @param properties Initial event properties (playback state)
     * @param context Event context (e.g., playlist, component info)
     * @param metadata Additional metadata
     */
    fun trackMedia(
        mediaID: String,
        properties: EventProperties? = null,
        context: EventContext? = null,
        metadata: Map<String, Any>? = null
    ) {
        stopHeartbeats()
        val isNewItem = itemID == null || mediaID != itemID
        itemID = mediaID
        props = properties
        this.context = context
        this.metadata = metadata
        trackingStartDate = Date()

        if (props == null) {
            props = EventProperties()
        }
        props?.playbackRate = if (player != null) player!!.playbackParameters.speed else 1f
        props?.playbackPosition = if (player != null) (player!!.currentPosition / 1000).toDouble() else 0.0
        if (isNewItem) {
            props?.timeSpent = 0.0
        }

        player?.let { p ->
            p.addListener(playerListener)
            if (p.isPlaying) {
                startHeartbeats()
            }
        }
    }

    /**
     * Stops tracking the current media item.
     */
    fun clearCurrentItem() {
        stopHeartbeats()
        player?.removeListener(playerListener)

        itemID = null
        props = null
        context = null
        metadata = null
        trackingStartDate = null
    }

    private fun startHeartbeats() {
        val collector = PeachCollector.shared ?: return

        for ((name, publisher) in collector.publishers) {
            if (heartbeatJobs[name]?.isActive == true) continue

            val intervalMs = publisher.playerTrackerHeartbeatInterval * 1000L
            heartbeatJobs[name] = scope.launch {
                while (isActive) {
                    delay(intervalMs)
                    if (props == null) continue
                    updateTimeSpent()
                    props?.playbackPosition = (player?.currentPosition?.div(1000))?.toDouble()
                    Event.sendMediaHeartbeat(itemID!!, props, context, metadata, name)
                }
            }
        }
    }

    private fun stopHeartbeats() {
        for ((_, job) in heartbeatJobs) {
            job.cancel()
        }
        heartbeatJobs.clear()
    }

    private fun updateTimeSpent() {
        if (trackingStartDate == null) return
        val now = Date()
        val diff = now.time - trackingStartDate!!.time
        props?.timeSpent = (diff / 1000).toDouble()
    }
}
