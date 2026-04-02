package ch.ebu.peachcollector

/**
 * Contextual metadata associated with an event.
 * Provides information about the source, collection, recommendation, or media context.
 */
class EventContext(
    var contextID: String? = null,
    var type: String? = null,
) {
    var itemID: String? = null
    var items: List<String>? = null
    var hitIndex: Int = -1
    var itemIndex: Int = -1
    var itemsCount: Int = -1
    var appSectionID: String? = null
    var source: String? = null
    var referrer: String? = null
    var component: EventContextComponent? = null
    var experimentID: String? = null
    var experimentComponent: String? = null

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
     * Converts this context to its JSON-publishable map representation.
     */
    fun toJsonMap(): Map<String, Any>? {
        val map = mutableMapOf<String, Any>()

        contextID?.let { map[PeachConstants.CONTEXT_ID_KEY] = it }
        type?.let { map[PeachConstants.CONTEXT_TYPE_KEY] = it }
        itemID?.let { map[PeachConstants.CONTEXT_ITEM_ID_KEY] = it }
        items?.let { map[PeachConstants.CONTEXT_ITEMS_KEY] = it }
        if (hitIndex >= 0) map[PeachConstants.CONTEXT_HIT_INDEX_KEY] = hitIndex
        if (itemIndex >= 0) map[PeachConstants.CONTEXT_ITEM_INDEX_KEY] = itemIndex
        if (itemsCount >= 0) map[PeachConstants.CONTEXT_ITEMS_COUNT_KEY] = itemsCount
        appSectionID?.let { map[PeachConstants.CONTEXT_PAGE_URI_KEY] = it }
        source?.let { map[PeachConstants.CONTEXT_SOURCE_KEY] = it }
        referrer?.let { map[PeachConstants.CONTEXT_REFERRER_KEY] = it }
        component?.toJsonMap()?.let { map[PeachConstants.CONTEXT_COMPONENT_KEY] = it }
        experimentID?.let { map[PeachConstants.CONTEXT_EXPERIMENT_ID_KEY] = it }
        experimentComponent?.let { map[PeachConstants.CONTEXT_EXPERIMENT_COMPONENT_KEY] = it }

        // Merge custom fields
        customFields?.let { map.putAll(it) }

        return map.ifEmpty { null }
    }

    companion object {

        /**
         * Creates a context for collection events.
         * Sets experiment defaults ("default"/"main") matching Java behavior.
         */
        @JvmStatic
        fun collectionContext(collectionID: String? = null, type: String? = null): EventContext {
            return EventContext(type = type).apply {
                experimentID = "default"
                experimentComponent = "main"
            }
        }

        /**
         * Creates a context for recommendation events.
         */
        @JvmStatic
        fun recommendationContext(recommendationID: String? = null, type: String? = null): EventContext {
            return EventContext(type = type)
        }

        /**
         * Creates a context for media events.
         */
        @JvmStatic
        fun mediaContext(mediaID: String, type: String? = null): EventContext {
            return EventContext(contextID = mediaID, type = type)
        }
    }
}
