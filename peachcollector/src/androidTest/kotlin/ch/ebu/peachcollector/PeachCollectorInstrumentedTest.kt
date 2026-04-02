package ch.ebu.peachcollector

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for PeachCollector.
 * Tests event collection, publishing, payload format, and configuration.
 */
@RunWith(AndroidJUnit4::class)
class PeachCollectorInstrumentedTest {

    companion object {
        const val PUBLISHER_NAME = "DefaultPublisher"
        const val PUBLISHER_NAME2 = "SecondPublisher"
        const val PUBLISHER_NAME3 = "ThirdPublisher"
    }

    private lateinit var context: Context
    private val receiver = LogReceiver()
    var currentEventType: String? = null

    @Before
    fun initializeModule() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        PeachCollector.isUnitTesting = true
        PeachCollector.shouldCollectAnonymousEvents = true
        PeachCollector.userIsLoggedIn = true

        val app = context.applicationContext as Application
        PeachCollector.init(app)
        PeachCollector.clean()

        val publisher = Publisher("zzebu00000000017")
        PeachCollector.addPublisher(publisher, PUBLISHER_NAME)

        val filter = IntentFilter(PeachConstants.PEACH_LOG_NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
    }

    @After
    fun tearDown() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    @Test
    fun testInitialization() {
        assertNotNull("PeachCollector is not initialized", PeachCollector.shared)
        assertTrue("Session start timestamp not set", PeachCollector.sessionStartTimestamp > 0)
        assertNotNull("Database not initialized", PeachCollector.shared!!.database)
        assertNotNull("Publishers not initialized", PeachCollector.shared!!.publishers)
        assertNotNull("Publisher was not added", PeachCollector.shared!!.publishers[PUBLISHER_NAME])
    }

    @Test
    fun testPublisherConfiguration() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 2
        publisher.maxEventsPerBatch = 3

        for (i in 0 until 3) {
            Event.sendPageView("page00$i", referrer = "reco00")
        }

        Thread.sleep(5000)
        assertTrue("The right number of events was sent", receiver.testSuccess)
    }

    @Test
    fun testPublisherRemoteConfiguration() {
        val publisher = Publisher("zzebu00000000017", "https://peach-bucket.ebu.io/zzebu/config-test.json")
        Thread.sleep(2000)
        assertEquals("Remote config maxEventsPerBatch set", 5, publisher.maxEventsPerBatch)
        assertEquals("Remote config maxEventsPerBatchAfterOfflineSession set", 500, publisher.maxEventsPerBatchAfterOfflineSession)
    }

    @Test
    fun testAppID() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        PeachCollector.appID = "test.app"
        currentEventType = "pageView"
        Event.sendPageView("page001", referrer = "reco00")

        Thread.sleep(2000)
        assertTrue("The custom app ID was set", receiver.testAppIDSuccess)
    }

    @Test
    fun testUserIDChange() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "userID"

        PeachCollector.userID = "123456789"
        Event.sendPageView("page001", referrer = "reco00")

        Thread.sleep(2000)
        assertTrue("The custom user ID was set", receiver.testUserID1Success)

        PeachCollector.userID = "12345678910"
        Event.sendPageView("page001", referrer = "reco00")

        Thread.sleep(2000)
        assertTrue("The custom user ID was changed", receiver.testUserID2Success)
    }

    @Test
    fun testCustomClientField() {
        val publisher = PeachCollector.sharedCollector!!.publishers.get(PUBLISHER_NAME)!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        publisher.addClientField("testField", "test")
        currentEventType = "customClientField"

        Event.sendPageView("page001", referrer = "reco00")

        Thread.sleep(2000)
        assertTrue("The custom client field was not set", receiver.testCustomClientField)
    }

    @Test
    fun testUserIsLoggedInChange() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "userLoggedIn"

        PeachCollector.userIsLoggedIn = true
        Event.sendPageView("page001", referrer = "reco00")

        Thread.sleep(2000)
        assertTrue("user_logged_in should be true", receiver.testUserLoggedIn)

        PeachCollector.userIsLoggedIn = false
        Event.sendPageView("page001", referrer = "reco00")

        Thread.sleep(2000)
        assertFalse("user_logged_in should be false", receiver.testUserLoggedIn)
    }

    @Test
    fun testWorkingPublisherWith1000Events() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 2

        for (i in 0 until 1000) {
            Event.sendPageView("page00$i", referrer = "reco00")
        }

        Thread.sleep(20000)
        assertEquals("The right number of events was sent (1000)", 1000, receiver.publishedEventsCount)
    }

    @Test
    fun testFailingPublisherWith1000Events() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.serviceURL = ""
        publisher.interval = 1
        publisher.maxEventsPerBatch = 2

        for (i in 0 until 1000) {
            Event.sendPageView("page00$i", referrer = "reco00")
        }

        Thread.sleep(1000)
        assertEquals(0, receiver.publishedEventsCount)

        publisher.serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=zzebu00000000017"
        Thread.sleep(10000)
        assertEquals("The right number of events was sent (1000)", 1000, receiver.publishedEventsCount)
    }

    @Test
    fun test3PublishersWith1000Events() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 2

        val publisher2 = Publisher("zzebu00000000017").apply {
            interval = 5
            maxEventsPerBatch = 5
        }
        PeachCollector.addPublisher(publisher2, PUBLISHER_NAME2)

        val publisher3 = Publisher("zzebu00000000017").apply {
            interval = 50
            maxEventsPerBatch = 1000
        }
        PeachCollector.addPublisher(publisher3, PUBLISHER_NAME3)

        for (i in 0 until 1000) {
            Event.sendPageView("page00$i", referrer = "reco00")
        }

        Thread.sleep(50000)
        assertEquals("Events sent to $PUBLISHER_NAME", 1000, receiver.publishedEventsCount)
        assertEquals("Events sent to $PUBLISHER_NAME2", 1000, receiver.publishedEventsCount2)
        assertEquals("Events sent to $PUBLISHER_NAME3", 1000, receiver.publishedEventsCount3)
    }

    @Test
    fun testCollectionHit() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "collectionHit"

        val ctx = EventContext.collectionContext().apply {
            component = EventContextComponent(
                type = "Carousel",
                name = "collectionCarousel",
                version = "1.0"
            )
        }
        Event.sendCollectionHit(
            collectionID = "collection00",
            itemID = "media01",
            hitIndex = 1,
            context = ctx
        )

        Thread.sleep(2000)
    }

    @Test
    fun testCollectionItemDisplayed() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "collectionItemDisplayed"

        val ctx = EventContext.collectionContext().apply {
            component = EventContextComponent(
                type = "Carousel",
                name = "collectionCarousel",
                version = "1.0"
            )
        }
        Event.sendCollectionItemDisplayed(
            itemID = "media01",
            collectionID = "collection00",
            itemIndex = 1,
            itemsCount = 12,
            context = ctx
        )

        Thread.sleep(2000)
    }

    @Test
    fun testRecommendationHit() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "recommendationHit"

        val ctx = EventContext.recommendationContext().apply {
            component = EventContextComponent(
                type = "Carousel",
                name = "recoCarousel",
                version = "1.0"
            )
        }
        Event.sendRecommendationHit(
            recommendationID = "reco00",
            itemID = "media01",
            hitIndex = 1,
            context = ctx
        )

        Thread.sleep(2000)
    }

    @Test
    fun testRecommendationDisplayed() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "recommendationDisplayed"

        val ctx = EventContext.recommendationContext().apply {
            component = EventContextComponent(
                type = "Carousel",
                name = "recoCarousel",
                version = "1.0"
            )
        }
        Event.sendRecommendationDisplayed(
            recommendationID = "reco00",
            items = listOf("media00", "media01", "media02", "media03"),
            context = ctx
        )

        Thread.sleep(2000)
    }

    @Test
    fun testMediaSeekEvent() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "mediaSeek"

        val ctx = EventContext.mediaContext("reco00", "playlist").apply {
            component = EventContextComponent(
                type = "player",
                name = "bottomPlayer",
                version = "1.0"
            )
            this.add("testKey", "testValue")
        }

        val props = EventProperties().apply {
            audioMode = AudioMode.NORMAL
            playbackPosition = 10.0
            previousPlaybackPosition = 5.0
            startMode = StartMode.NORMAL
            isPlaying = false
        }

        Event.sendMediaSeek("media01", props, ctx, null)

        Thread.sleep(2000)
    }

    @Test
    fun testMaxEvents() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.serviceURL = ""
        publisher.interval = 1
        publisher.maxEventsPerBatch = 500

        for (i in 0 until 500) {
            Event.sendPageView("page$i", referrer = "reco00")
        }

        // Wait for all event-inserting coroutines to complete
        Thread.sleep(3000)
        assertEquals(0, receiver.publishedEventsCount)

        PeachCollector.maximumStoredEvents = 250
        PeachCollector.checkStoredEvents()
        Thread.sleep(1000)

        currentEventType = "pageViewMax"
        publisher.serviceURL = "https://pipe-collect.ebu.io/v3/collect?s=zzebu00000000017"
        Thread.sleep(30000)
        assertEquals("The right number of events was sent (250)", 250, receiver.publishedEventsCount)
        assertTrue(receiver.testMaxSuccess)
    }

    // region LogReceiver

    inner class LogReceiver : BroadcastReceiver() {
        var testSuccess = false
        var publishedEventsCount = 0
        var publishedEventsCount2 = 0
        var publishedEventsCount3 = 0
        var testMaxSuccess = false
        var testAppIDSuccess = false
        var testUserID1Success = false
        var testUserID2Success = false
        var testUserLoggedIn = false
        var testCustomClientField = false
        var payloadPageViewSuccess = false
        var payloadRecoHitSuccess = false
        var payloadCustomSuccess = false

        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(PeachConstants.PEACH_LOG_NOTIFICATION_MESSAGE)
            val payload = intent?.getStringExtra(PeachConstants.PEACH_LOG_NOTIFICATION_PAYLOAD)

            if (msg != null) {
                if (msg.contains("3 events")) {
                    testSuccess = true
                }

                if (msg.startsWith("Published")) {
                    val numbersInMessage = msg.replace(Regex("[\\D]"), "")
                    if (numbersInMessage.isNotEmpty()) {
                        val count = numbersInMessage.toIntOrNull() ?: 0
                        when {
                            msg.contains(PUBLISHER_NAME3) -> publishedEventsCount3 += count
                            msg.contains(PUBLISHER_NAME2) -> publishedEventsCount2 += count
                            msg.contains(PUBLISHER_NAME) -> publishedEventsCount += count
                        }
                    }
                }
            }
            if (payload != null && currentEventType != null) {
                Log.e("PEACH COLLECTOR", "payload received")
                try {
                    processPayload(payload)
                } catch (e: Exception) {
                    Log.e("PEACH COLLECTOR", "Error processing payload", e)
                }
            }
        }

        private fun processPayload(payload: String) {
            val json = JSONObject(payload)

            when (currentEventType?.lowercase()) {
                "collectionitemdisplayed" -> {
                    val client = json.getJSONObject("client")
                    assertTrue(client.getBoolean("user_logged_in"))
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)
                    assertEquals("collection_item_displayed", event.getString("type"))
                    assertEquals("collection00", event.getString("id"))
                    val ctx = event.getJSONObject("context")
                    assertEquals(1, ctx.getInt("item_index"))
                    assertEquals(12, ctx.getInt("items_count"))
                    assertEquals("media01", ctx.getString("item_id"))
                    assertEquals("default", ctx.getString("experiment_id"))
                    assertEquals("main", ctx.getString("experiment_component"))
                }
                "collectionhit" -> {
                    val client = json.getJSONObject("client")
                    assertTrue(client.getBoolean("user_logged_in"))
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)
                    assertEquals("collection_hit", event.getString("type"))
                    assertTrue(event.getLong("event_timestamp") < System.currentTimeMillis())
                    assertTrue(event.getLong("event_timestamp") > System.currentTimeMillis() - 10000)
                    assertEquals("collection00", event.getString("id"))
                    val ctx = event.getJSONObject("context")
                    assertEquals(1, ctx.getInt("hit_index"))
                    assertEquals("media01", ctx.getString("item_id"))
                    assertEquals("default", ctx.getString("experiment_id"))
                    assertEquals("main", ctx.getString("experiment_component"))
                    val component = ctx.getJSONObject("component")
                    assertEquals("collectionCarousel", component.getString("name"))
                    assertEquals("Carousel", component.getString("type"))
                    assertEquals("1.0", component.getString("version"))
                }
                "recommendationhit" -> {
                    val client = json.getJSONObject("client")
                    assertTrue(client.getBoolean("user_logged_in"))
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)
                    assertEquals("recommendation_hit", event.getString("type"))
                    assertTrue(event.getLong("event_timestamp") < System.currentTimeMillis())
                    assertTrue(event.getLong("event_timestamp") > System.currentTimeMillis() - 10000)
                    assertEquals("reco00", event.getString("id"))
                    val ctx = event.getJSONObject("context")
                    assertEquals(1, ctx.getInt("hit_index"))
                    assertEquals("media01", ctx.getString("item_id"))
                    val component = ctx.getJSONObject("component")
                    assertEquals("recoCarousel", component.getString("name"))
                    assertEquals("Carousel", component.getString("type"))
                    assertEquals("1.0", component.getString("version"))
                }
                "recommendationdisplayed" -> {
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)
                    assertEquals("recommendation_displayed", event.getString("type"))
                    assertTrue(event.getLong("event_timestamp") < System.currentTimeMillis())
                    assertTrue(event.getLong("event_timestamp") > System.currentTimeMillis() - 10000)
                    assertEquals("reco00", event.getString("id"))
                    val ctx = event.getJSONObject("context")
                    val items = ctx.getJSONArray("items")
                    assertEquals("media00", items.getString(0))
                    assertEquals("media01", items.getString(1))
                    val component = ctx.getJSONObject("component")
                    assertEquals("recoCarousel", component.getString("name"))
                    assertEquals("Carousel", component.getString("type"))
                    assertEquals("1.0", component.getString("version"))
                }
                "mediaseek" -> {
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)
                    assertEquals("media_seek", event.getString("type"))
                    assertTrue(event.getLong("event_timestamp") < System.currentTimeMillis())
                    assertTrue(event.getLong("event_timestamp") > System.currentTimeMillis() - 10000)
                    assertEquals("media01", event.getString("id"))
                    val ctx = event.getJSONObject("context")
                    assertEquals("reco00", ctx.getString("id"))
                    assertEquals("playlist", ctx.getString("type"))
                    assertEquals("testValue", ctx.getString("testKey"))
                    val props = event.getJSONObject("props")
                    assertEquals(5.0, props.getDouble("previous_playback_position_s"), 0.01)
                    assertEquals(10.0, props.getDouble("playback_position_s"), 0.01)
                    assertEquals("normal", props.getString("start_mode"))
                    assertEquals("normal", props.getString("audio_mode"))
                    assertFalse(props.getBoolean("is_playing"))
                    val component = ctx.getJSONObject("component")
                    assertEquals("bottomPlayer", component.getString("name"))
                    assertEquals("player", component.getString("type"))
                    assertEquals("1.0", component.getString("version"))
                }
                "pageviewmax" -> {
                    val events = json.getJSONArray("events")
                    assertEquals(250, events.length())
                    for (i in 0 until events.length()) {
                        val event = events.getJSONObject(i)
                        if (event.getString("id") == "page400") {
                            testMaxSuccess = true
                        }
                    }
                }
                "pageview" -> {
                    val client = json.getJSONObject("client")
                    if (client.getString("app_id") == "test.app") {
                        testAppIDSuccess = true
                    }
                }
                "userid" -> {
                    val userId = json.getString("user_id")
                    if (userId == "123456789") testUserID1Success = true
                    if (userId == "12345678910") testUserID2Success = true
                }
                "userloggedin" -> {
                    val client = json.getJSONObject("client")
                    testUserLoggedIn = client.getBoolean("user_logged_in")
                }
                "customclientfield" -> {
                    val client = json.getJSONObject("client")
                    testCustomClientField = client.getString("testField") == "test"
                }
                "payloadpageview" -> {
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)
                    Log.i("Event", json.toString())
                    // Event-level fields
                    assertEquals("page_view", event.getString("type"))
                    assertEquals("page001", event.getString("id"))
                    assertTrue(event.has("event_timestamp"))

                    // Context
                    val ctx = event.getJSONObject("context")
                    assertEquals("reco001", ctx.getString("id"))
                    assertEquals("previousPage", ctx.getString("referrer"))
                    // Java's EventContext() has null experiment fields — must NOT be present
                    assertFalse(ctx.has("experiment_id"))
                    assertFalse(ctx.has("experiment_component"))
                    // Only 2 keys in context
                    assertEquals(2, ctx.length())

                    // No props or metadata
                    assertFalse(event.has("props"))
                    assertFalse(event.has("metadata"))

                    payloadPageViewSuccess = true
                }
                "payloadrecohit" -> {
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)

                    assertEquals("recommendation_hit", event.getString("type"))
                    assertEquals("reco001", event.getString("id"))

                    val ctx = event.getJSONObject("context")
                    assertEquals("media01", ctx.getString("item_id"))
                    assertEquals(2, ctx.getInt("hit_index"))
                    assertEquals("appSection01", ctx.getString("page_uri"))
                    assertEquals("homeSource", ctx.getString("source"))

                    val comp = ctx.getJSONObject("component")
                    assertEquals("Carousel", comp.getString("type"))
                    assertEquals("recoCarousel", comp.getString("name"))
                    assertEquals("1.0", comp.getString("version"))

                    // Java's recommendationContext does NOT set experiment or contextID
                    assertFalse(ctx.has("experiment_id"))
                    assertFalse(ctx.has("experiment_component"))
                    assertFalse(ctx.has("id"))

                    assertFalse(event.has("props"))
                    assertFalse(event.has("metadata"))

                    payloadRecoHitSuccess = true
                }
                "payloadcustom" -> {
                    val events = json.getJSONArray("events")
                    assertEquals(1, events.length())
                    val event = events.getJSONObject(0)

                    assertEquals("custom_media_event", event.getString("type"))
                    assertEquals("media001", event.getString("id"))

                    // Context
                    val ctx = event.getJSONObject("context")
                    assertEquals("media001", ctx.getString("id"))
                    assertEquals("audio", ctx.getString("type"))
                    assertEquals("customContextValue", ctx.getString("customContextKey"))
                    assertEquals(42, ctx.getInt("customContextNumber"))
                    assertFalse(ctx.has("experiment_id"))
                    assertFalse(ctx.has("experiment_component"))

                    val comp = ctx.getJSONObject("component")
                    assertEquals("player", comp.getString("type"))
                    assertEquals("mainPlayer", comp.getString("name"))
                    assertEquals("2.0", comp.getString("version"))

                    // Properties
                    val props = event.getJSONObject("props")
                    assertEquals("normal", props.getString("audio_mode"))
                    assertEquals(120.0, props.getDouble("playback_position_s"), 0.01)
                    assertEquals(60.0, props.getDouble("previous_playback_position_s"), 0.01)
                    assertEquals(1.0, props.getDouble("playback_rate"), 0.01)
                    assertEquals(0.8, props.getDouble("volume"), 0.01)
                    assertEquals(true, props.getBoolean("is_playing"))
                    assertEquals("normal", props.getString("start_mode"))
                    assertEquals("customPropValue", props.getString("customPropKey"))
                    assertEquals(true, props.getBoolean("customPropBool"))

                    // Metadata
                    val meta = event.getJSONObject("metadata")
                    assertEquals("metaValue1", meta.getString("metaKey1"))
                    assertEquals(99, meta.getInt("metaKey2"))

                    payloadCustomSuccess = true
                }
            }
        }
    }

    // endregion

    // region Payload Format Parity Tests

    /**
     * Verifies that event JSON payloads match the Java PeachCollector output exactly.
     * Events are sent through the real pipeline and captured via broadcast.
     * Expected values are derived from tracing the Java code with identical inputs.
     */
    @Test
    fun testPageViewPayloadMatchesJava() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "payloadPageView"

        Event.sendPageView("page001", "previousPage", "reco001")

        Thread.sleep(3000)
        assertTrue("PageView payload was not received", receiver.payloadPageViewSuccess)
    }

    @Test
    fun testRecommendationHitPayloadMatchesJava() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "payloadRecoHit"

        val component = EventContextComponent(
            type = "Carousel",
            name = "recoCarousel",
            version = "1.0"
        )
        val ctx = EventContext.recommendationContext().apply {
            appSectionID = "appSection01"
            source = "homeSource"
            this.component = component
        }

        Event.sendRecommendationHit(
            recommendationID = "reco001",
            itemID = "media01",
            hitIndex = 2,
            context = ctx
        )

        Thread.sleep(3000)
        assertTrue("RecommendationHit payload was not received", receiver.payloadRecoHitSuccess)
    }

    @Test
    fun testCustomEventPayloadMatchesJava() {
        val publisher = PeachCollector.shared!!.publishers[PUBLISHER_NAME]!!
        publisher.interval = 1
        publisher.maxEventsPerBatch = 1
        currentEventType = "payloadCustom"

        val playerComponent = EventContextComponent(
            type = "player",
            name = "mainPlayer",
            version = "2.0"
        )
        val ctx = EventContext.mediaContext("media001", "audio").apply {
            component = playerComponent
            add("customContextKey", "customContextValue")
            add("customContextNumber", 42)
        }

        val props = EventProperties().apply {
            audioMode = AudioMode.NORMAL
            playbackPosition = 120.0
            previousPlaybackPosition = 60.0
            playbackRate = 1f
            volume = 0.8f
            isPlaying = true
            startMode = StartMode.NORMAL
            add("customPropKey", "customPropValue")
            add("customPropBool", true)
        }

        val metadata = mapOf<String, Any>("metaKey1" to "metaValue1", "metaKey2" to 99)

        Event.send("custom_media_event", "media001", props, ctx, metadata)

        Thread.sleep(3000)
        assertTrue("Custom event payload was not received", receiver.payloadCustomSuccess)
    }

    @Test
    fun testCollectionContextHasExperimentDefaults() {
        // Java's collectionContext factory sets experimentID="default", experimentComponent="main"
        val ctx = EventContext.collectionContext()
        ctx.items = listOf("item1", "item2")
        ctx.appSectionID = "section01"

        val ctxMap = ctx.toJsonMap()!!
        assertEquals("default", ctxMap["experiment_id"])
        assertEquals("main", ctxMap["experiment_component"])
        assertEquals(listOf("item1", "item2"), ctxMap["items"])
        assertEquals("section01", ctxMap["page_uri"])
        assertFalse("context id should not be present for collection", ctxMap.containsKey("id"))
    }

    @Test
    fun testCustomFieldNullRemoval() {
        val ctx = EventContext()
        ctx.add("key1", "value1")
        ctx.add("key2", "value2")
        assertEquals("value1", ctx.get("key1"))

        ctx.add("key1", null as String?)
        assertNull(ctx.get("key1"))
        assertEquals("value2", ctx.get("key2"))

        val props = EventProperties()
        props.add("propKey", "propValue")
        assertEquals("propValue", props.get("propKey"))
        props.add("propKey", null as String?)
        assertNull(props.get("propKey"))
    }

    // endregion
}
