package ch.ebu.peachcollector

/**
 * All constants used by the PeachCollector framework.
 * JSON keys, event types, status codes, and configuration keys.
 */
object PeachConstants {

    // region Broadcasting
    const val PEACH_LOG_NOTIFICATION = "ch.ebu.testingLog"
    const val PEACH_LOG_NOTIFICATION_MESSAGE = "message"
    const val PEACH_LOG_NOTIFICATION_PAYLOAD = "payload"
    // endregion

    // region SharedPreferences Keys
    const val SESSION_START_TIMESTAMP_SPREF_KEY = "peach_session_start_timestamp"
    const val SESSION_ID_SPREF_KEY = "peach_session_id"
    const val SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY = "peach_last_active_timestamp"
    const val DEVICE_ID_SPREF_KEY = "peach_device_id"
    // endregion

    // region Schema
    const val PEACH_SCHEMA_VERSION = "1.0.3"
    const val PEACH_SCHEMA_VERSION_KEY = "peach_schema_version"
    const val PEACH_FRAMEWORK_VERSION_KEY = "peach_framework_version"
    const val PEACH_IMPLEMENTATION_VERSION_KEY = "peach_implementation_version"
    // endregion

    // region Top-Level Payload Keys
    const val SESSION_START_TIMESTAMP_KEY = "session_start_timestamp"
    const val SESSION_ID_KEY = "session_id"
    const val SENT_TIMESTAMP_KEY = "sent_timestamp"
    const val CLIENT_KEY = "client"
    const val USER_ID_KEY = "user_id"
    const val EVENTS_KEY = "events"
    // endregion

    // region Event Keys
    const val EVENT_TYPE_KEY = "type"
    const val EVENT_ID_KEY = "id"
    const val EVENT_TIMESTAMP_KEY = "event_timestamp"
    const val EVENT_CONTEXT_KEY = "context"
    const val EVENT_PROPS_KEY = "props"
    const val EVENT_METADATA_KEY = "metadata"
    // endregion

    // region Context Keys
    const val CONTEXT_ID_KEY = "id"
    const val CONTEXT_TYPE_KEY = "type"
    const val CONTEXT_ITEMS_KEY = "items"
    const val CONTEXT_ITEM_ID_KEY = "item_id"
    const val CONTEXT_HIT_INDEX_KEY = "hit_index"
    const val CONTEXT_ITEM_INDEX_KEY = "item_index"
    const val CONTEXT_ITEMS_COUNT_KEY = "items_count"
    const val CONTEXT_PAGE_URI_KEY = "page_uri"
    const val CONTEXT_SOURCE_KEY = "source"
    const val CONTEXT_REFERRER_KEY = "referrer"
    const val CONTEXT_COMPONENT_KEY = "component"
    const val CONTEXT_COMPONENT_TYPE_KEY = "type"
    const val CONTEXT_COMPONENT_NAME_KEY = "name"
    const val CONTEXT_COMPONENT_VERSION_KEY = "version"
    const val CONTEXT_EXPERIMENT_ID_KEY = "experiment_id"
    const val CONTEXT_EXPERIMENT_COMPONENT_KEY = "experiment_component"
    // endregion

    // region Media Property Keys
    const val PROPS_PLAYLIST_ID_KEY = "playlist_id"
    const val PROPS_INSERT_POSITION_KEY = "insert_position"
    const val PROPS_TIME_SPENT_KEY = "time_spent_s"
    const val PROPS_PLAYBACK_POSITION_KEY = "playback_position_s"
    const val PROPS_PREVIOUS_PLAYBACK_POSITION_KEY = "previous_playback_position_s"
    const val PROPS_IS_PLAYING_KEY = "is_playing"
    const val PROPS_PREVIOUS_ID_KEY = "previous_id"
    const val PROPS_PLAYBACK_RATE_KEY = "playback_rate"
    const val PROPS_VOLUME_KEY = "volume"
    const val PROPS_VIDEO_MODE_KEY = "video_mode"
    const val PROPS_AUDIO_MODE_KEY = "audio_mode"
    const val PROPS_START_MODE_KEY = "start_mode"
    // endregion

    // region Client Keys
    const val CLIENT_ID_KEY = "id"
    const val CLIENT_TYPE_KEY = "type"
    const val CLIENT_TYPE_VALUE = "mobileapp"
    const val CLIENT_APP_ID_KEY = "app_id"
    const val CLIENT_NAME_KEY = "name"
    const val CLIENT_VERSION_KEY = "version"
    const val CLIENT_USER_LOGGED_IN_KEY = "user_logged_in"
    const val CLIENT_DEVICE_KEY = "device"
    const val CLIENT_OS_KEY = "os"
    // endregion

    // region Device Keys
    const val DEVICE_TYPE_KEY = "type"
    const val DEVICE_VENDOR_KEY = "vendor"
    const val DEVICE_MODEL_KEY = "model"
    const val DEVICE_SCREEN_SIZE_KEY = "screen_size"
    const val DEVICE_LANGUAGE_KEY = "language"
    const val DEVICE_TIMEZONE_KEY = "timezone"
    // endregion

    // region OS Keys
    const val OS_NAME_KEY = "name"
    const val OS_NAME_VALUE = "Android"
    const val OS_VERSION_KEY = "version"
    // endregion
}

// region Event Types
object EventType {
    const val MEDIA_PLAY = "media_play"
    const val MEDIA_PAUSE = "media_pause"
    const val MEDIA_SEEK = "media_seek"
    const val MEDIA_STOP = "media_stop"
    const val MEDIA_END = "media_end"
    const val MEDIA_HEARTBEAT = "media_heartbeat"
    const val MEDIA_VIDEO_MODE_CHANGED = "media_video_mode_changed"
    const val MEDIA_AUDIO_MODE_CHANGED = "media_audio_mode_changed"
    const val MEDIA_AUDIO_CHANGED = "media_audio_changed"
    const val MEDIA_PLAYLIST_ADD = "media_playlist_add"
    const val MEDIA_PLAYLIST_REMOVE = "media_playlist_remove"
    const val MEDIA_LIKE = "media_like"
    const val MEDIA_SHARE = "media_share"
    const val RECOMMENDATION_LOADED = "recommendation_loaded"
    const val RECOMMENDATION_DISPLAYED = "recommendation_displayed"
    const val RECOMMENDATION_HIT = "recommendation_hit"
    const val COLLECTION_LOADED = "collection_loaded"
    const val COLLECTION_DISPLAYED = "collection_displayed"
    const val COLLECTION_ITEM_DISPLAYED = "collection_item_displayed"
    const val COLLECTION_HIT = "collection_hit"
    const val ARTICLE_START = "article_start"
    const val ARTICLE_END = "article_end"
    const val ARTICLE_READ = "article_read"
    const val READ_MORE = "read_more"
    const val PAGE_VIEW = "page_view"
}
// endregion

// region Status
object Status {
    const val QUEUED = 0
    const val SENT_TO_PUBLISHER = 1
    const val PUBLISHED = 2
}
// endregion

// region Enums
enum class GoBackOnlinePolicy {
    SEND_ALL,
    SEND_ALL_RANDOMLY
}

object VideoMode {
    const val BAR = "bar"
    const val MINI = "mini"
    const val NORMAL = "normal"
    const val WIDE = "wide"
    const val PIP = "pip"
    const val FULLSCREEN = "fullscreen"
    const val CAST = "cast"
    const val PREVIEW = "preview"
}

object AudioMode {
    const val NORMAL = "normal"
    const val BACKGROUND = "background"
    const val MUTED = "muted"
}

object StartMode {
    const val NORMAL = "normal"
    const val AUTO_PLAY = "auto_play"
    const val AUTO_CONTINUE = "auto_continue"
}

object InsertPosition {
    const val TOP = "top"
    const val END = "end"
}

object ClientDeviceType {
    const val PHONE = "phone"
    const val TABLET = "tablet"
}

object MetadataType {
    const val AUDIO = "audio"
    const val VIDEO = "video"
    const val ARTICLE = "article"
    const val PAGE = "page"
}

object MetadataFormat {
    const val DEMAND = "ondemand"
    const val LIVE = "live"
    const val DVR = "dvr"
}
