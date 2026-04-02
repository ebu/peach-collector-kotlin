package ch.ebu.peachcollector.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.*
import java.util.Date

/**
 * Room type converters for Date and Map<String, Any> serialization.
 * Uses kotlinx.serialization JsonElement as intermediate for type-safe
 * conversion of heterogeneous maps.
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    // region Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
    // endregion

    // region Map<String, Any> converters
    @TypeConverter
    fun fromStringMap(map: Map<String, @JvmSuppressWildcards Any>?): String? {
        if (map == null) return null
        val jsonObject = mapToJsonObject(map)
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val jsonObject = json.parseToJsonElement(value).jsonObject
        return jsonObjectToMap(jsonObject)
    }
    // endregion

    companion object {
        /**
         * Converts a Map<String, Any> to a kotlinx JsonObject.
         */
        fun mapToJsonObject(map: Map<String, Any?>): JsonObject {
            val content = mutableMapOf<String, JsonElement>()
            for ((key, value) in map) {
                content[key] = anyToJsonElement(value)
            }
            return JsonObject(content)
        }

        /**
         * Converts a kotlinx JsonObject to a Map<String, Any>.
         */
        fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            for ((key, element) in jsonObject) {
                val value = jsonElementToAny(element)
                if (value != null) {
                    map[key] = value
                }
            }
            return map
        }

        /**
         * Converts any Kotlin value to a JsonElement.
         */
        fun anyToJsonElement(value: Any?): JsonElement = when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                mapToJsonObject(value as Map<String, Any?>)
            }
            is List<*> -> {
                JsonArray(value.map { anyToJsonElement(it) })
            }
            else -> JsonPrimitive(value.toString())
        }

        /**
         * Converts a JsonElement back to a Kotlin value.
         */
        fun jsonElementToAny(element: JsonElement): Any? = when (element) {
            is JsonNull -> null
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" || element.content == "false" -> element.boolean
                    element.content.contains('.') -> element.double
                    else -> {
                        val longVal = element.long
                        if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) longVal.toInt() else longVal
                    }
                }
            }
            is JsonObject -> jsonObjectToMap(element)
            is JsonArray -> element.map { jsonElementToAny(it) }
        }
    }
}
