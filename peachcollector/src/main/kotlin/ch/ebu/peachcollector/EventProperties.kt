package ch.ebu.peachcollector

/**
 * Properties associated with media events.
 * Captures playback state, position, and mode information.
 */
class EventProperties {

    var playlistID: String? = null
    var insertPosition: String? = null
    var timeSpent: Double? = null
    var playbackPosition: Double? = null
    var previousPlaybackPosition: Double? = null
    var isPlaying: Boolean? = null
    var previousMediaID: String? = null
    var playbackRate: Float? = null
    var volume: Float? = null
    var videoMode: String? = null
    var audioMode: String? = null
    var startMode: String? = null

    private var customFields: MutableMap<String, Any>? = null

    // region Custom Fields

    fun add(key: String, value: String?) {
        addObject(key, value)
    }

    fun add(key: String, value: Number?) {
        addObject(key, value)
    }

    fun add(key: String, value: Boolean?) {
        addObject(key, value)
    }

    private fun addObject(key: String, value: Any?) {
        if (value == null) {
            remove(key)
            return
        }
        if (customFields == null) {
            customFields = mutableMapOf()
        }
        customFields!![key] = value
    }

    fun remove(key: String) {
        if (customFields != null && customFields!!.containsKey(key)) {
            customFields!!.remove(key)
            if (customFields!!.isEmpty()) customFields = null
        }
    }

    fun get(key: String): Any? {
        return customFields?.get(key)
    }

    // endregion

    /**
     * Converts these properties to their JSON-publishable map representation.
     */
    fun toJsonMap(): Map<String, Any>? {
        val map = mutableMapOf<String, Any>()

        playlistID?.let { map[PeachConstants.PROPS_PLAYLIST_ID_KEY] = it }
        insertPosition?.let { map[PeachConstants.PROPS_INSERT_POSITION_KEY] = it }
        timeSpent?.let { map[PeachConstants.PROPS_TIME_SPENT_KEY] = it }
        playbackPosition?.let { map[PeachConstants.PROPS_PLAYBACK_POSITION_KEY] = it }
        previousPlaybackPosition?.let { map[PeachConstants.PROPS_PREVIOUS_PLAYBACK_POSITION_KEY] = it }
        isPlaying?.let { map[PeachConstants.PROPS_IS_PLAYING_KEY] = it }
        previousMediaID?.let { map[PeachConstants.PROPS_PREVIOUS_ID_KEY] = it }
        playbackRate?.let { map[PeachConstants.PROPS_PLAYBACK_RATE_KEY] = it }
        volume?.let { map[PeachConstants.PROPS_VOLUME_KEY] = it }
        videoMode?.let { map[PeachConstants.PROPS_VIDEO_MODE_KEY] = it }
        audioMode?.let { map[PeachConstants.PROPS_AUDIO_MODE_KEY] = it }
        startMode?.let { map[PeachConstants.PROPS_START_MODE_KEY] = it }

        // Merge custom fields
        customFields?.let { map.putAll(it) }

        return map.ifEmpty { null }
    }
}
