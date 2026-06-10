package ch.ebu.peachcollector

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import ch.ebu.peachcollector.db.PeachDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Main entry point for the PeachCollector event collection framework.
 * Manages event queuing, storage, and delivery to registered publishers.
 *
 * Initialize with [PeachCollector.init] and register publishers with [addPublisher].
 */
class PeachCollector private constructor(private val application: Application) {

    internal val database: PeachDatabase = PeachDatabase.getInstance(application)
    val publishers: MutableMap<String, Publisher> = ConcurrentHashMap()

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val publisherJobs: MutableMap<String, Job> = mutableMapOf()
    private val publisherFailures: MutableMap<String, Int> = mutableMapOf()
    private val publisherMutexes: MutableMap<String, Mutex> = mutableMapOf()

    val applicationContext: Context
        get() = application.applicationContext

    private val prefs: SharedPreferences
        get() = application.getSharedPreferences("peach_collector_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PeachCollector"

        @Volatile
        var shared: PeachCollector? = null
            private set

        @JvmStatic
        val sharedCollector: PeachCollector?
            get() = shared

        // region Configuration Properties

        var implementationVersion: String? = null

        var userID: String? = null

        var appID: String? = null

        var userIsLoggedIn: Boolean? = null
            set(value) {
                field = value
                invalidatePublishersClientInfo()
            }

        var shouldCollectAnonymousEvents: Boolean = false

        var isUnitTesting: Boolean = false

        var inactivityInterval: Long = 1_800_000L // 30 minutes in ms

        var sessionStartTimestamp: Long = 0L

        var sessionID: String = ""

        var maximumStorageDays: Int = 30

        var maximumStoredEvents: Int = 5000

        private var _deviceID: String? = null
        private var limitedTrackingEnabled: Boolean = false

        // endregion

        // region Initialization

        /**
         * Initializes the PeachCollector singleton.
         * Must be called once from Application.onCreate() or Activity.
         */
        fun init(application: Application): PeachCollector {
            return shared ?: synchronized(this) {
                shared ?: PeachCollector(application).also { collector ->
                    shared = collector
                    collector.setupSession()
                    collector.setupLifecycleObserver()
                    collector.fetchDeviceID()
                }
            }
        }

        // endregion

        // region Event Sending

        /**
         * Queues an event for delivery to all registered publishers.
         */
        fun sendEvent(event: Event) {
            val collector = shared ?: return
            if (!canCollectEvents()) return

            collector.scope.launch {
                try {
                    val eventRowID = collector.database.eventDao().insertEvent(event)

                    for ((name, publisher) in collector.publishers) {
                        if (publisher.shouldProcessEvent(event)) {
                            collector.database.eventDao().insertStatus(
                                EventStatus(
                                    eventID = eventRowID.toInt(),
                                    publisherName = name,
                                    status = Status.QUEUED
                                )
                            )
                        }
                    }

                    if (isUnitTesting) {
                        broadcastLog("Event queued: ${event.type} (${event.eventID})")
                    }

                    collector.checkPublishers()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to queue event", e)
                }
            }
        }

        /**
         * Queues an event for delivery to a specific publisher.
         */
        fun sendEvent(event: Event, publisherName: String) {
            val collector = shared ?: return
            if (!canCollectEvents()) return

            collector.scope.launch {
                try {
                    val eventRowID = collector.database.eventDao().insertEvent(event)
                    val publisher = collector.publishers[publisherName] ?: return@launch

                    if (publisher.shouldProcessEvent(event)) {
                        collector.database.eventDao().insertStatus(
                            EventStatus(
                                eventID = eventRowID.toInt(),
                                publisherName = publisherName,
                                status = Status.QUEUED
                            )
                        )
                    }

                    if (isUnitTesting) {
                        broadcastLog("Event queued for $publisherName: ${event.type} (${event.eventID})")
                    }

                    collector.checkPublishers()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to queue event", e)
                }
            }
        }

        // endregion

        // region Publisher Management

        /**
         * Registers a publisher with the given name.
         */
        fun addPublisher(publisher: Publisher, name: String) {
            val collector = shared ?: return
            collector.publishers[name] = publisher

            // Reset statuses and send any queued events for this publisher
            collector.scope.launch {
                // Reset any in-progress statuses back to queued (in case of crash)
                val statuses = collector.database.eventDao().getStatuses(name)
                for (status in statuses) {
                    if (status.status == Status.SENT_TO_PUBLISHER) {
                        collector.database.eventDao().updateStatus(status.eventID, name, Status.QUEUED)
                    }
                }
                // Send any pending events
                collector.sendEventsToPublisher(name)
            }
        }

        /**
         * Forces all publishers to refresh their cached client info.
         */
        fun invalidatePublishersClientInfo() {
            val collector = shared ?: return
            for ((_, publisher) in collector.publishers) {
                publisher.invalidateClientInfo()
            }
        }

        // endregion

        // region Flush & Clean

        /**
         * Immediately sends all pending events to all publishers.
         */
        fun flush() {
            val collector = shared ?: return
            collector.scope.launch {
                for ((name, _) in collector.publishers) {
                    collector.sendEventsToPublisher(name)
                }
            }
        }

        /**
         * Clears all queued events from the database.
         */
        fun clean() {
            val collector = shared ?: return
            // Cancel all pending publisher jobs
            for ((_, job) in collector.publisherJobs) {
                job.cancel()
            }
            collector.publisherJobs.clear()
            collector.publisherFailures.clear()
            collector.publisherMutexes.clear()
            collector.publishers.clear()

            runBlocking {
                collector.database.eventDao().deleteAllEvents()
                collector.database.eventDao().deleteAllStatuses()
            }
        }

        /**
         * Enforces maximum stored events limit.
         */
        internal fun checkStoredEvents() {
            val collector = shared ?: return
            collector.scope.launch {
                collector.cleanOldEvents()
            }
        }

        // endregion

        // region Device ID

        fun getDeviceID(): String {
            return if (limitedTrackingEnabled || _deviceID == null) "Anonymous" else _deviceID!!
        }

        fun setDeviceID(id: String) {
            _deviceID = id
            limitedTrackingEnabled = false
            invalidatePublishersClientInfo()
        }

        // endregion

        // region Session Management

        /**
         * Checks if the app has been inactive longer than the inactivity interval.
         * If so, resets the session.
         */
        fun checkInactivity() {
            val collector = shared ?: return
            val currentTimestamp = System.currentTimeMillis()
            val sPrefs = collector.prefs

            // Load session from prefs
            sessionStartTimestamp = sPrefs.getLong(
                PeachConstants.SESSION_START_TIMESTAMP_SPREF_KEY, currentTimestamp
            )
            sessionID = sPrefs.getString(
                PeachConstants.SESSION_ID_SPREF_KEY, UUID.randomUUID().toString()
            ) ?: UUID.randomUUID().toString()
            // Default to 0 (not currentTimestamp) so the very first launch is treated as
            // inactive, forcing a reset that persists the session. Matches iOS, where the
            // baseline (lastRecordedEventTimestamp) defaults to 0.
            val lastActiveTimestamp = sPrefs.getLong(
                PeachConstants.SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY, 0L
            )

            if (currentTimestamp - lastActiveTimestamp > inactivityInterval) {
                sessionStartTimestamp = currentTimestamp
                sessionID = UUID.randomUUID().toString()
                sPrefs.edit()
                    .putLong(PeachConstants.SESSION_START_TIMESTAMP_SPREF_KEY, sessionStartTimestamp)
                    .putString(PeachConstants.SESSION_ID_SPREF_KEY, sessionID)
                    .apply()
            }

            // Always update last active timestamp
            sPrefs.edit()
                .putLong(PeachConstants.SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY, currentTimestamp)
                .apply()
        }

        // endregion

        // region Internal Helpers

        private fun canCollectEvents(): Boolean {
            if (!shouldCollectAnonymousEvents) {
                val deviceId = getDeviceID()
                if (deviceId == "Anonymous" && userID == null) return false
            }
            return true
        }

        private fun broadcastLog(message: String) {
            val collector = shared ?: return
            val intent = Intent(PeachConstants.PEACH_LOG_NOTIFICATION).apply {
                setPackage(collector.applicationContext.packageName)
                putExtra(PeachConstants.PEACH_LOG_NOTIFICATION_MESSAGE, message)
            }
            collector.applicationContext.sendBroadcast(intent)
        }

        internal fun broadcastLogWithPayload(message: String, payload: String) {
            val collector = shared ?: return
            val intent = Intent(PeachConstants.PEACH_LOG_NOTIFICATION).apply {
                setPackage(collector.applicationContext.packageName)
                putExtra(PeachConstants.PEACH_LOG_NOTIFICATION_MESSAGE, message)
                putExtra(PeachConstants.PEACH_LOG_NOTIFICATION_PAYLOAD, payload)
            }
            collector.applicationContext.sendBroadcast(intent)
        }

        // endregion
    }

    // region Instance Methods

    private fun setupSession() {
        // Load the persisted session and evaluate inactivity, matching iOS: a fresh session
        // is minted and persisted on first launch, and restored on subsequent launches until
        // the inactivity interval is exceeded.
        checkInactivity()

        // Clean old events
        scope.launch {
            cleanOldEvents()
        }
    }

    private fun setupLifecycleObserver() {
        // ProcessLifecycleOwner must be observed on the main thread
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(PeachLifecycleObserver())
        }
    }

    private fun fetchDeviceID() {
        if (_deviceID != null) return

        // Try advertising ID in background (matching Java priority: Ad ID → stored → generate)
        scope.launch {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                limitedTrackingEnabled = adInfo.isLimitAdTrackingEnabled
                if (!limitedTrackingEnabled && _deviceID == null) {
                    _deviceID = adInfo.id
                    if (_deviceID == null) {
                        // Ad ID unavailable, try stored UUID
                        val savedID = prefs.getString(PeachConstants.DEVICE_ID_SPREF_KEY, null)
                        _deviceID = savedID ?: UUID.randomUUID().toString()

                        // Save to prefs only when Ad ID is unavailable
                        prefs.edit().putString(PeachConstants.DEVICE_ID_SPREF_KEY, _deviceID).apply()
                    }
                }
            } catch (_: Exception) {
                // Google Play Services not available
            }
        }
    }

    private suspend fun cleanOldEvents() {
        val dateLimit = System.currentTimeMillis() - (maximumStorageDays.toLong() * 24 * 60 * 60 * 1000)
        database.eventDao().deleteEventsBefore(dateLimit)

        // Check max stored events
        val allEvents = database.eventDao().getAllEvents()
        if (allEvents.size > maximumStoredEvents) {
            val excess = allEvents.size - maximumStoredEvents
            database.eventDao().deleteEvents(0, excess)
        }
    }

    internal suspend fun checkPublishers() {
        for ((name, publisher) in publishers) {
            val pendingStatuses = database.eventDao().getPendingStatuses(name)
            val pendingCount = pendingStatuses.size

            if (pendingCount == 0) continue

            if (publisher.interval == 0 || pendingCount >= publisher.maxEventsPerBatch) {
                // Send immediately
                sendEventsToPublisher(name)
            } else {
                // Schedule if not already scheduled
                if (publisherJobs[name]?.isActive != true) {
                    schedulePublisher(name, publisher.interval * 1000L)
                }
            }
        }
    }

    private fun schedulePublisher(publisherName: String, delayMs: Long) {
        publisherJobs[publisherName]?.cancel()
        publisherJobs[publisherName] = scope.launch {
            delay(delayMs)
            sendEventsToPublisher(publisherName)
        }
    }

    internal suspend fun sendEventsToPublisher(publisherName: String) {
        val mutex = synchronized(publisherMutexes) {
            publisherMutexes.getOrPut(publisherName) { Mutex() }
        }
        mutex.withLock {
            sendEventsToPublisherInternal(publisherName)
        }
    }

    private suspend fun sendEventsToPublisherInternal(publisherName: String) {
        val publisher = publishers[publisherName] ?: return
        val dao = database.eventDao()

        val pendingStatuses = dao.getPendingStatuses(publisherName)
        if (pendingStatuses.isEmpty()) return

        // Batch events
        val batchSize = publisher.maxEventsPerBatchAfterOfflineSession
        val batch = pendingStatuses.take(batchSize)

        // Load events
        val events = batch.mapNotNull { status ->
            dao.getEvent(status.eventID)
        }

        if (events.isEmpty()) return

        // Mark as sent
        for (status in batch) {
            dao.updateStatus(status.eventID, publisherName, Status.SENT_TO_PUBLISHER)
        }

        // Send
        val success = publisher.processEvents(events)

        if (success) {
            if (isUnitTesting) {
                val payload = buildPublisherPayloadForLog(publisher, events)
                broadcastLogWithPayload(
                    "Published ${events.size} events to $publisherName",
                    payload
                )
            }

            // Mark as published and clean up
            for (status in batch) {
                dao.updateStatus(status.eventID, publisherName, Status.PUBLISHED)

                // Delete event if all publishers have published it
                val remaining = dao.getPendingEventStatuses(status.eventID)
                if (remaining.isEmpty()) {
                    val event = dao.getEvent(status.eventID)
                    if (event != null) dao.deleteEvent(event)
                }
            }

            publisherFailures[publisherName] = 0

            // Check if more events to send
            val morePending = dao.getPendingStatuses(publisherName)
            if (morePending.isNotEmpty()) {
                sendEventsToPublisherInternal(publisherName)
            }
        } else {
            // Revert to queued
            for (status in batch) {
                dao.updateStatus(status.eventID, publisherName, Status.QUEUED)
            }

            val failures = (publisherFailures[publisherName] ?: 0) + 1
            publisherFailures[publisherName] = failures

            // Exponential backoff
            val backoffMs = minOf(300_000L, publisher.interval.toLong() * 1000L * (failures + 1))
            schedulePublisher(publisherName, backoffMs)

            if (isUnitTesting) {
                broadcastLogWithPayload(
                    "Failed to publish ${events.size} events to $publisherName (attempt $failures)",
                    ""
                )
            }
        }
    }

    private fun buildPublisherPayloadForLog(publisher: Publisher, events: List<Event>): String {
        try {
            val payload = mutableMapOf<String, Any>(
                PeachConstants.PEACH_SCHEMA_VERSION_KEY to PeachConstants.PEACH_SCHEMA_VERSION,
                PeachConstants.PEACH_FRAMEWORK_VERSION_KEY to "${BuildConfig.VERSION_NAME}b${BuildConfig.VERSION_CODE}",
                PeachConstants.SESSION_START_TIMESTAMP_KEY to sessionStartTimestamp,
                PeachConstants.SESSION_ID_KEY to sessionID,
                PeachConstants.SENT_TIMESTAMP_KEY to System.currentTimeMillis(),
                PeachConstants.CLIENT_KEY to publisher.clientInfo(),
                PeachConstants.EVENTS_KEY to events.map { it.toJsonMap() }
            )
            implementationVersion?.let {
                payload[PeachConstants.PEACH_IMPLEMENTATION_VERSION_KEY] = it
            }
            userID?.let {
                payload[PeachConstants.USER_ID_KEY] = it
            }

            val jsonObject = ch.ebu.peachcollector.db.Converters.mapToJsonObject(payload)
            return kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(), jsonObject
            )
        } catch (_: Exception) {
            return ""
        }
    }

    // endregion
}
