package ch.ebu.peachcollector

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import ch.ebu.peachcollector.db.Converters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Defines an endpoint and behavior for publishing collected events.
 * Events are posted as JSON to the configured service URL.
 */
open class Publisher {

    var serviceURL: String? = null
    var remoteConfigurationURL: String? = null
    var interval: Int = 20
    var maxEventsPerBatch: Int = 20
    var maxEventsPerBatchAfterOfflineSession: Int = 1000
    var playerTrackerHeartbeatInterval: Int = 5
    var gotBackPolicy: GoBackOnlinePolicy = GoBackOnlinePolicy.SEND_ALL

    private var cachedClientInfo: Map<String, Any>? = null
    private val customClientFields: MutableMap<String, Any> = mutableMapOf()

    // Remote configuration (JSON object cached from remote URL)
    private var remoteConfiguration: org.json.JSONObject? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    constructor()

    constructor(siteKey: String) {
        serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=$siteKey"
    }

    constructor(siteKey: String, remoteConfigURL: String) : this(siteKey) {
        if (siteKey.isNotEmpty() && remoteConfigURL.isNotEmpty()) {
            remoteConfigurationURL = remoteConfigURL

            val appContext = PeachCollector.shared?.applicationContext
            if (appContext != null) {
                val sPrefs = appContext.getSharedPreferences("peach_publisher_prefs", Context.MODE_PRIVATE)
                val textJson = sPrefs.getString(remoteConfigurationURL, null)
                if (textJson != null) {
                    try {
                        remoteConfiguration = org.json.JSONObject(textJson)
                    } catch (_: org.json.JSONException) {}
                }
            }
            checkConfig()
        }
    }

    // region Event Filtering

    /**
     * Determines whether this publisher should process the given event.
     * Checks against remote configuration filters if available.
     */
    open fun shouldProcessEvent(event: Event): Boolean {
        if (remoteConfiguration != null && remoteConfiguration!!.has("filter")) {
            try {
                val eventsFilter = remoteConfiguration!!.getJSONArray("filter")
                for (i in 0 until eventsFilter.length()) {
                    if (eventsFilter.getString(i).equals(event.type, ignoreCase = true)) {
                        return true
                    }
                }
            } catch (_: org.json.JSONException) {
                return false
            }
            return false
        }
        return !serviceURL.isNullOrEmpty()
    }

    // endregion

    // region Custom Client Fields

    fun addClientField(key: String, value: Any) {
        customClientFields[key] = value
        invalidateClientInfo()
    }

    fun removeCustomClientField(key: String) {
        customClientFields.remove(key)
        invalidateClientInfo()
    }

    fun getCustomClientField(key: String): Any? = customClientFields[key]

    fun invalidateClientInfo() {
        cachedClientInfo = null
    }

    // endregion

    // region Client Info

    /**
     * Builds the client information payload.
     */
    fun clientInfo(): Map<String, Any> {
        cachedClientInfo?.let { return it }

        val context = PeachCollector.shared?.applicationContext ?: return emptyMap()

        val client = mutableMapOf<String, Any>(
            PeachConstants.CLIENT_ID_KEY to PeachCollector.getDeviceID(),
            PeachConstants.CLIENT_TYPE_KEY to PeachConstants.CLIENT_TYPE_VALUE,
            PeachConstants.CLIENT_DEVICE_KEY to deviceInfo(context),
            PeachConstants.CLIENT_OS_KEY to osInfo()
        )

        val appId = PeachCollector.appID ?: context.packageName
        client[PeachConstants.CLIENT_APP_ID_KEY] = appId

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
            client[PeachConstants.CLIENT_NAME_KEY] = appName

            val versionName = packageInfo.versionName ?: "unknown"
            @Suppress("DEPRECATION")
            val versionCode = packageInfo.versionCode
            client[PeachConstants.CLIENT_VERSION_KEY] = "${versionName}b${versionCode}"
        } catch (_: PackageManager.NameNotFoundException) {
            // Ignore
        }

        PeachCollector.userIsLoggedIn?.let {
            client[PeachConstants.CLIENT_USER_LOGGED_IN_KEY] = it
        }

        // Add custom fields
        client.putAll(customClientFields)

        cachedClientInfo = client
        return client
    }

    // endregion

    // region Publishing

    /**
     * Sends events to the service URL as an HTTP POST.
     * Returns true on success (HTTP 201), false otherwise.
     */
    suspend fun processEvents(events: List<Event>): Boolean {
        val url = serviceURL ?: return false

        val payload = buildPayload(events)
        val jsonString = serializePayload(payload)

        if (BuildConfig.DEBUG) {
            android.util.Log.d("PAYLOAD_SENT", jsonString)
        }

        return postJson(url, jsonString)
    }

    private fun buildPayload(events: List<Event>): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>(
            PeachConstants.PEACH_SCHEMA_VERSION_KEY to PeachConstants.PEACH_SCHEMA_VERSION,
            PeachConstants.PEACH_FRAMEWORK_VERSION_KEY to frameworkVersion(),
            PeachConstants.SESSION_START_TIMESTAMP_KEY to PeachCollector.sessionStartTimestamp,
            PeachConstants.SESSION_ID_KEY to PeachCollector.sessionID,
            PeachConstants.SENT_TIMESTAMP_KEY to System.currentTimeMillis(),
            PeachConstants.CLIENT_KEY to clientInfo(),
            PeachConstants.EVENTS_KEY to events.map { it.toJsonMap() }
        )

        PeachCollector.implementationVersion?.let {
            payload[PeachConstants.PEACH_IMPLEMENTATION_VERSION_KEY] = it
        }

        PeachCollector.userID?.let {
            payload[PeachConstants.USER_ID_KEY] = it
        }

        return payload
    }

    private fun frameworkVersion(): String {
        return "${BuildConfig.VERSION_NAME}b${BuildConfig.VERSION_CODE}"
    }

    private fun serializePayload(payload: Map<String, Any?>): String {
        val jsonObject = Converters.mapToJsonObject(payload)
        return Json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    private suspend fun postJson(url: String, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            response.use { it.code == 201 }
        } catch (e: Exception) {
            android.util.Log.w("PeachCollector", "Failed to post events to $url", e)
            false
        }
    }

    // endregion

    // region Remote Configuration

    /**
     * Checks and applies remote configuration.
     * If cached config is expired or missing, fetches from remote URL.
     */
    private fun checkConfig() {
        val url = remoteConfigurationURL ?: return
        val appContext = PeachCollector.shared?.applicationContext ?: return

        val expiryDateKey = url + "_date"
        val sPrefs = appContext.getSharedPreferences("peach_publisher_prefs", Context.MODE_PRIVATE)
        val currentTimestamp = System.currentTimeMillis()
        val expiryTimestamp = sPrefs.getLong(expiryDateKey, currentTimestamp)

        if (currentTimestamp > expiryTimestamp) {
            remoteConfiguration = null
        }

        if (remoteConfiguration != null) {
            applyRemoteConfig()
        } else {
            PeachCollector.shared?.scope?.launch { fetchRemoteConfiguration() }
        }
    }

    /**
     * Fetches remote configuration from the configured URL.
     */
    suspend fun fetchRemoteConfiguration() {
        val url = remoteConfigurationURL ?: return

        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()
                response.use {
                    if (it.isSuccessful) {
                        val body = it.body?.string() ?: return@withContext
                        try {
                            remoteConfiguration = org.json.JSONObject(body)
                        } catch (_: org.json.JSONException) {
                            return@withContext
                        }

                        // Cache the config with expiry based on max_cache_hours
                        var maxCacheHours = 1.0
                        try {
                            maxCacheHours = remoteConfiguration!!.getDouble("max_cache_hours")
                        } catch (_: org.json.JSONException) {}

                        val appContext = PeachCollector.shared?.applicationContext ?: return@withContext
                        val currentTimestamp = System.currentTimeMillis()
                        val expiryTimestamp = (currentTimestamp + (maxCacheHours * 60 * 60 * 1000) + 10000).toLong()
                        val sPrefs = appContext.getSharedPreferences("peach_publisher_prefs", Context.MODE_PRIVATE)

                        sPrefs.edit()
                            .putString(url, remoteConfiguration.toString())
                            .putLong(url + "_date", expiryTimestamp)
                            .apply()

                        applyRemoteConfig()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("PeachCollector", "Failed to fetch remote configuration from $url", e)
            }
        }
    }

    private fun applyRemoteConfig() {
        val config = remoteConfiguration ?: return
        try {
            if (config.has("max_batch_size")) {
                maxEventsPerBatch = config.getInt("max_batch_size").coerceIn(1, 1000)
            }
            if (config.has("max_events_per_request")) {
                maxEventsPerBatchAfterOfflineSession = config.getInt("max_events_per_request").coerceIn(1, 5000)
            }
            if (config.has("flush_interval_sec")) {
                interval = config.getInt("flush_interval_sec").coerceIn(1, 3600)
            }
            if (config.has("heartbeat_frequency_sec")) {
                playerTrackerHeartbeatInterval = config.getInt("heartbeat_frequency_sec").coerceIn(1, 300)
            }
        } catch (_: org.json.JSONException) {
            // Use default values
        }
    }

    // endregion

    companion object {

        /**
         * Returns device information map.
         */
        fun deviceInfo(context: Context): Map<String, Any> {
            val info = mutableMapOf<String, Any>(
                PeachConstants.DEVICE_VENDOR_KEY to "${Build.MANUFACTURER}, ${Build.BRAND}",
                PeachConstants.DEVICE_MODEL_KEY to "${Build.MODEL}, ${Build.PRODUCT}",
                PeachConstants.DEVICE_LANGUAGE_KEY to Locale.getDefault().displayLanguage,
            )

            // Device type
            val isTablet = context.resources.getBoolean(R.bool.isTablet)
            info[PeachConstants.DEVICE_TYPE_KEY] = if (isTablet) ClientDeviceType.TABLET else ClientDeviceType.PHONE

            // Screen size
            var screenResolution = "unknown"
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                val display = wm.defaultDisplay
                @Suppress("DEPRECATION")
                display.getMetrics(metrics)

                var widthPixels = metrics.widthPixels
                var heightPixels = metrics.heightPixels

                try {
                    val realSize = android.graphics.Point()
                    @Suppress("DEPRECATION")
                    display.getRealSize(realSize)
                    widthPixels = realSize.x
                    heightPixels = realSize.y
                } catch (_: Exception) { }

                screenResolution = "${widthPixels}x${heightPixels}"
            } catch (_: Exception) {
                // Ignore
            }
            info[PeachConstants.DEVICE_SCREEN_SIZE_KEY] = screenResolution

            // Timezone (DST-aware)
            val tz = TimeZone.getDefault()
            val offsetMs = tz.getOffset(System.currentTimeMillis())
            val offsetHours = offsetMs.toDouble() / 3_600_000.0
            info[PeachConstants.DEVICE_TIMEZONE_KEY] = offsetHours

            return info
        }

        /**
         * Returns OS information map.
         */
        fun osInfo(): Map<String, Any> {
            return mapOf(
                PeachConstants.OS_NAME_KEY to PeachConstants.OS_NAME_VALUE,
                PeachConstants.OS_VERSION_KEY to "${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL}) API${Build.VERSION.SDK_INT}"
            )
        }
    }
}
