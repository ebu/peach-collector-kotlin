# PeachCollector for Android (Kotlin)

The **PeachCollector** framework provides simple functionalities to facilitate the collection of analytics events. It manages a queue of events serialized and stored locally until they are successfully published to configured endpoints.

This is the Kotlin rewrite of [peach-collector-android](https://github.com/ebu/peach-collector-android), built with coroutines, Room, and Media3.

## Compatibility

- Minimum SDK: **21** (Android 5.0)
- Compile SDK: **36**
- Built with Kotlin, coroutines, and AndroidX

## Setup

### 1. Add the Gradle dependency

Add JitPack to your root `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your module's `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.ebu:peach-collector-kotlin:2.0.0")
}
```

### 2. Initialize the collector

Initialize PeachCollector in your `Application.onCreate()`:
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PeachCollector.shouldCollectAnonymousEvents = true
        PeachCollector.init(this)
    }
}
```

### 3. Add a publisher

Add a `Publisher` to start sending queued events. Provide a **SiteKey** or a custom **URL**:
```kotlin
val publisher = Publisher("zzebu00000000017")
PeachCollector.addPublisher(publisher, "My Publisher")
```

## Configuring the Collector

All configuration properties are set on the `PeachCollector` companion object:

```kotlin
// User identification
PeachCollector.userID = "123e4567-e89b-12d3-a456-426655440000"
PeachCollector.setDeviceID(UUID.randomUUID().toString())
PeachCollector.userIsLoggedIn = true

// App identification
PeachCollector.appID = "my.test.app"
PeachCollector.implementationVersion = "1"

// Collection behavior
PeachCollector.shouldCollectAnonymousEvents = true
PeachCollector.isUnitTesting = true

// Session and storage
PeachCollector.inactivityInterval = 3_600_000L  // 1 hour in ms (default: 30 min)
PeachCollector.maximumStorageDays = 5            // default: 30
PeachCollector.maximumStoredEvents = 1000        // default: 5000
```

> [!NOTE]
> - `maximumStorageDays` and `maximumStoredEvents` should be set **before** calling `init()`.
> - The collector retrieves the Advertising ID as the device ID. If tracking is limited and no `userID` is set, events are not collected unless `shouldCollectAnonymousEvents` is `true`.
> - If `setDeviceID()` is called before `init()`, the framework will not attempt to retrieve an Advertising ID.


## Configuring a Publisher

```kotlin
val publisher = Publisher("zzebu00000000017").apply {
    interval = 30                              // seconds between sends (default: 20)
    maxEventsPerBatch = 50                     // triggers send when reached (default: 20)
    maxEventsPerBatchAfterOfflineSession = 500 // max per request (default: 1000)
    playerTrackerHeartbeatInterval = 10        // heartbeat interval in seconds (default: 5)
    gotBackPolicy = GoBackOnlinePolicy.SEND_ALL
}
PeachCollector.addPublisher(publisher, "My Publisher")
```

| Property | Default | Description |
|---|---|---|
| `interval` | 20 | Seconds between publish cycles. 0 = send immediately. |
| `maxEventsPerBatch` | 20 | Event count that triggers early publishing. |
| `maxEventsPerBatchAfterOfflineSession` | 1000 | Max events per single HTTP request. |
| `playerTrackerHeartbeatInterval` | 5 | Seconds between heartbeat events during media playback. |
| `gotBackPolicy` | `SEND_ALL` | Behavior after offline period. Options: `SEND_ALL`, `SEND_ALL_RANDOMLY`. |

### Custom client fields

Add custom fields to the client information sent with every request:
```kotlin
publisher.addClientField("country", "Germany")
publisher.addClientField("isGeolocalized", true)
```

## Remote Configuration

A publisher can load its configuration from a remote JSON file:
```kotlin
val publisher = Publisher("zzebu00000000017", "https://example.com/config.json")
PeachCollector.addPublisher(publisher, "RemotePublisher")
```

The remote JSON can configure: `max_batch_size`, `max_events_per_request`, `flush_interval_sec`, `heartbeat_frequency_sec`, `filter` (array of event types to accept), and `max_cache_hours`.

## Flushing and Cleaning

```kotlin
PeachCollector.flush()  // Send all queued events immediately
PeachCollector.clean()  // Remove all queued events
```

`flush()` is called automatically when the app goes to background. `clean()` is never called automatically.

## Recording Events

### Page view
```kotlin
Event.sendPageView("home_page", referrer = "login_page", recommendationID = "reco001")
```

### Recommendation events
```kotlin
// Recommendation displayed
val component = EventContextComponent(type = "carousel", name = "MainCarousel", version = "1.0")
val context = EventContext.recommendationContext().apply {
    this.component = component
}
Event.sendRecommendationDisplayed(
    recommendationID = "recoList01",
    items = listOf("media00", "media01", "media02"),
    context = context
)

// Recommendation hit
Event.sendRecommendationHit(
    recommendationID = "recoList01",
    itemID = "media01",
    hitIndex = 1,
    context = context
)
```

### Collection events
```kotlin
val context = EventContext.collectionContext().apply {
    appSectionID = "home"
    source = "editorial"
    component = EventContextComponent(type = "grid", name = "MainGrid", version = "1.0")
}
Event.sendCollectionLoaded("collection01", items = listOf("item1", "item2"), context = context)
Event.sendCollectionDisplayed("collection01", items = listOf("item1"), context = context)
Event.sendCollectionHit("collection01", itemID = "item1", hitIndex = 0, context = context)
Event.sendCollectionItemDisplayed("item1", collectionID = "collection01", itemIndex = 0, itemsCount = 10)
```

### Media events 
> [!IMPORTANT]  
> We recommend using the `PlayerTracker` whenever possible instead of sending events manually.

```kotlin
val props = EventProperties().apply {
    audioMode = AudioMode.NORMAL
    playbackPosition = 120.0
    startMode = StartMode.NORMAL
}
val context = EventContext.mediaContext("media001", "audio").apply {
    component = EventContextComponent(type = "player", name = "MainPlayer", version = "2.0")
}

Event.sendMediaPlay("media001", props, context, null)
Event.sendMediaPause("media001", props, context, null)
Event.sendMediaSeek("media001", props, context, null)
Event.sendMediaStop("media001", props, context, null)
Event.sendMediaEnd("media001", props, context, null)
Event.sendMediaPlaylistAdd("media001", props, context, null)
Event.sendMediaPlaylistRemove("media001", props, context, null)
```

### Custom events
```kotlin
val context = EventContext().apply {
    add("customKey", "customValue")
}
val props = EventProperties().apply {
    add("score", 42)
}
Event.send("custom_event_type", "event001", props, context, mapOf("source" to "app"))
```

### Custom fields on EventContext and EventProperties

Both support typed custom fields:
```kotlin
context.add("key", "stringValue")
context.add("key", 42)
context.add("key", true)
context.remove("key")
val value = context.get("key")
```
Adding `null` removes the field.

## Player Tracker

`PeachPlayerTracker` automatically tracks Media3 ExoPlayer state and sends play, pause, seek, end, and heartbeat events.

```kotlin
// Set the player instance
PeachPlayerTracker.setPlayer(exoPlayer)

// Start tracking a media item
val properties = EventProperties().apply {
    videoMode = VideoMode.NORMAL
}
val context = EventContext.mediaContext("video001").apply {
    component = EventContextComponent(
        type = "media_player",
        name = "MainPlayer",
        version = "2.3.0"
    )
}
PeachPlayerTracker.trackMedia(
    mediaID = "video001",
    properties = properties,
    context = context
)

// Stop tracking
PeachPlayerTracker.clearCurrentItem()
```

The tracker automatically:
- Sends `media_play` when playback starts
- Sends `media_pause` when paused
- Sends `media_seek` on position discontinuities
- Sends `media_end` when playback reaches the end
- Sends `media_heartbeat` at the configured interval per publisher
- Updates `playback_position_s`, `time_spent_s`, `playback_rate`, and `volume` on properties

## Constants

### Event Types (`EventType`)
`MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_SEEK`, `MEDIA_STOP`, `MEDIA_END`, `MEDIA_HEARTBEAT`, `MEDIA_VIDEO_MODE_CHANGED`, `MEDIA_AUDIO_MODE_CHANGED`, `MEDIA_AUDIO_CHANGED`, `MEDIA_PLAYLIST_ADD`, `MEDIA_PLAYLIST_REMOVE`, `MEDIA_LIKE`, `MEDIA_SHARE`, `RECOMMENDATION_LOADED`, `RECOMMENDATION_DISPLAYED`, `RECOMMENDATION_HIT`, `COLLECTION_LOADED`, `COLLECTION_DISPLAYED`, `COLLECTION_ITEM_DISPLAYED`, `COLLECTION_HIT`, `ARTICLE_START`, `ARTICLE_END`, `ARTICLE_READ`, `READ_MORE`, `PAGE_VIEW`

### Media Constants
| Object | Values |
|---|---|
| `VideoMode` | `BAR`, `MINI`, `NORMAL`, `WIDE`, `PIP`, `FULLSCREEN`, `CAST`, `PREVIEW` |
| `AudioMode` | `NORMAL`, `BACKGROUND`, `MUTED` |
| `StartMode` | `NORMAL`, `AUTO_PLAY`, `AUTO_CONTINUE` |
| `InsertPosition` | `TOP`, `END` |
| `MetadataType` | `AUDIO`, `VIDEO`, `ARTICLE`, `PAGE` |
| `MetadataFormat` | `DEMAND`, `LIVE`, `DVR` |

## Java Interoperability

All public companion object methods are annotated with `@JvmStatic` and can be called from Java:
```java
PeachCollector.init(application);
Event.sendPageView("page001", "referrer", "reco001");
Publisher publisher = new Publisher("zzebu00000000017");
PeachCollector.addPublisher(publisher, "My Publisher");
```

## Migrating from the Java Version

See [MIGRATION.md](MIGRATION.md) for a detailed guide on API changes when upgrading from `peach-collector-android`.


