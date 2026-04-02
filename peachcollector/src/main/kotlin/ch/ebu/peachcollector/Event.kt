package ch.ebu.peachcollector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a collected event.
 * Events are created via companion object factory methods, stored in the database,
 * and published to registered publishers.
 */
@Entity(tableName = "Event")
data class Event(
    @ColumnInfo(name = "eventID") val eventID: String,
    @ColumnInfo(name = "type") val type: String,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "creationDate")
    var creationDate: Long = System.currentTimeMillis()

    @ColumnInfo(name = "pageStartDate")
    var pageStartDate: Long = 0L

    @ColumnInfo(name = "properties")
    var properties: Map<String, @JvmSuppressWildcards Any>? = null

    @ColumnInfo(name = "context")
    var context: Map<String, @JvmSuppressWildcards Any>? = null

    @ColumnInfo(name = "metadata")
    var metadata: Map<String, @JvmSuppressWildcards Any>? = null

    /**
     * Converts this event to its JSON-publishable representation.
     */
    fun toJsonMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            PeachConstants.EVENT_TYPE_KEY to type,
            PeachConstants.EVENT_ID_KEY to eventID,
            PeachConstants.EVENT_TIMESTAMP_KEY to creationDate
        )
        context?.let { map[PeachConstants.EVENT_CONTEXT_KEY] = it }
        properties?.let { map[PeachConstants.EVENT_PROPS_KEY] = it }
        metadata?.let { map[PeachConstants.EVENT_METADATA_KEY] = it }
        return map
    }

    companion object {

        // region Generic Event

        /**
         * Send a custom event. Event will be added to the queue and sent to all publishers.
         */
        @JvmStatic
        fun send(
            type: String,
            eventID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) {
            val event = Event(eventID, type).apply {
                this.properties = properties?.toJsonMap()
                this.context = context?.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event)
        }

        /**
         * Send a custom event to a specific publisher.
         */
        @JvmStatic
        fun send(
            type: String,
            eventID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null,
            publisherName: String
        ) {
            val event = Event(eventID, type).apply {
                this.properties = properties?.toJsonMap()
                this.context = context?.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event, publisherName)
        }

        // endregion

        // region Page Events

        @JvmStatic
        fun sendPageView(
            pageID: String,
            referrer: String? = null,
            recommendationID: String? = null
        ) {
            val context = EventContext()
            context.referrer = referrer
            context.contextID = recommendationID
            send(EventType.PAGE_VIEW, pageID, null, context, null)
        }

        // endregion

        // region Media Events

        @JvmStatic
        fun sendMediaPlay(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_PLAY, mediaID, properties, context, metadata)

        @JvmStatic
        fun sendMediaPause(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_PAUSE, mediaID, properties, context, metadata)

        @JvmStatic
        fun sendMediaSeek(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_SEEK, mediaID, properties, context, metadata)

        @JvmStatic
        fun sendMediaStop(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_STOP, mediaID, properties, context, metadata)

        @JvmStatic
        fun sendMediaEnd(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_END, mediaID, properties, context, metadata)

        @JvmStatic
        fun sendMediaHeartbeat(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null,
            publisherName: String? = null
        ) {
            val event = createMediaEvent(EventType.MEDIA_HEARTBEAT, mediaID, properties, context, metadata)
            if (publisherName != null) {
                PeachCollector.sendEvent(event, publisherName)
            } else {
                PeachCollector.sendEvent(event)
            }
        }

        @JvmStatic
        fun sendMediaPlaylistAdd(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_PLAYLIST_ADD, mediaID, properties, context, metadata)

        @JvmStatic
        fun sendMediaPlaylistRemove(
            mediaID: String,
            properties: EventProperties? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendMediaEvent(EventType.MEDIA_PLAYLIST_REMOVE, mediaID, properties, context, metadata)

        private fun sendMediaEvent(
            type: String,
            mediaID: String,
            properties: EventProperties?,
            context: EventContext?,
            metadata: Map<String, Any>?
        ) {
            PeachCollector.sendEvent(createMediaEvent(type, mediaID, properties, context, metadata))
        }

        private fun createMediaEvent(
            type: String,
            mediaID: String,
            properties: EventProperties?,
            context: EventContext?,
            metadata: Map<String, Any>?
        ): Event {
            return Event(mediaID, type).apply {
                this.properties = properties?.toJsonMap()
                this.context = context?.toJsonMap()
                this.metadata = metadata
            }
        }

        // endregion

        // region Collection Events

        @JvmStatic
        fun sendCollectionLoaded(
            collectionID: String,
            items: List<String>? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendCollectionEvent(EventType.COLLECTION_LOADED, collectionID, items, context, metadata)

        @JvmStatic
        fun sendCollectionDisplayed(
            collectionID: String,
            items: List<String>? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendCollectionEvent(EventType.COLLECTION_DISPLAYED, collectionID, items, context, metadata)

        @JvmStatic
        fun sendCollectionItemDisplayed(
            itemID: String,
            collectionID: String,
            itemIndex: Int = -1,
            itemsCount: Int = -1,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) {
            val ctx = context ?: EventContext.collectionContext()
            ctx.itemID = itemID
            if (itemIndex >= 0) ctx.itemIndex = itemIndex
            if (itemsCount >= 0) ctx.itemsCount = itemsCount

            val event = Event(collectionID, EventType.COLLECTION_ITEM_DISPLAYED).apply {
                this.context = ctx.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event)
        }

        @JvmStatic
        fun sendCollectionHit(
            collectionID: String,
            itemID: String,
            hitIndex: Int = -1,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) {
            val ctx = context ?: EventContext.collectionContext()
            ctx.itemID = itemID
            if (hitIndex >= 0) ctx.hitIndex = hitIndex

            val event = Event(collectionID, EventType.COLLECTION_HIT).apply {
                this.context = ctx.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event)
        }

        private fun sendCollectionEvent(
            type: String,
            collectionID: String,
            items: List<String>?,
            context: EventContext?,
            metadata: Map<String, Any>?
        ) {
            val ctx = context ?: EventContext.collectionContext()
            if (items != null) ctx.items = items

            val event = Event(collectionID, type).apply {
                this.context = ctx.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event)
        }

        // endregion

        // region Recommendation Events

        @JvmStatic
        fun sendRecommendationLoaded(
            recommendationID: String,
            items: List<String>? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendRecommendationEvent(EventType.RECOMMENDATION_LOADED, recommendationID, items, context, metadata)

        @JvmStatic
        fun sendRecommendationDisplayed(
            recommendationID: String,
            items: List<String>? = null,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) = sendRecommendationEvent(EventType.RECOMMENDATION_DISPLAYED, recommendationID, items, context, metadata)

        @JvmStatic
        fun sendRecommendationHit(
            recommendationID: String,
            itemID: String,
            hitIndex: Int = -1,
            context: EventContext? = null,
            metadata: Map<String, Any>? = null
        ) {
            val ctx = context ?: EventContext.recommendationContext()
            ctx.itemID = itemID
            if (hitIndex >= 0) ctx.hitIndex = hitIndex

            val event = Event(recommendationID, EventType.RECOMMENDATION_HIT).apply {
                this.context = ctx.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event)
        }

        private fun sendRecommendationEvent(
            type: String,
            recommendationID: String,
            items: List<String>?,
            context: EventContext?,
            metadata: Map<String, Any>?
        ) {
            val ctx = context ?: EventContext.recommendationContext()
            if (items != null) ctx.items = items

            val event = Event(recommendationID, type).apply {
                this.context = ctx.toJsonMap()
                this.metadata = metadata
            }
            PeachCollector.sendEvent(event)
        }

        // endregion
    }
}
