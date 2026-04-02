package ch.ebu.peachcollector

/**
 * Describes a UI component that generated an event (e.g., carousel, player).
 */
data class EventContextComponent(
    var type: String? = null,
    var name: String? = null,
    var version: String? = null
) {
    fun toJsonMap(): Map<String, Any>? {
        val map = mutableMapOf<String, Any>()
        type?.let { map[PeachConstants.CONTEXT_COMPONENT_TYPE_KEY] = it }
        name?.let { map[PeachConstants.CONTEXT_COMPONENT_NAME_KEY] = it }
        version?.let { map[PeachConstants.CONTEXT_COMPONENT_VERSION_KEY] = it }
        return map.ifEmpty { null }
    }
}
