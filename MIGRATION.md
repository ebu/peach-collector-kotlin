# Migration Guide: PeachCollector Android (Java) to the Kotlin version

This guide covers the key differences when migrating from `PeachCollector-android` (Java) to `PeachCollector-android-kotlin`.

---

## Constants Reorganization

Constants have been restructured from the nested `Constant` class into top-level objects:

| Old                                                 | New                             |
|-----------------------------------------------------|---------------------------------|
| `Constant.EventType.MediaPlay`                      | `EventType.MEDIA_PLAY`          |
| `Constant.EventType.MediaPause`                     | `EventType.MEDIA_PAUSE`         |
| `Constant.EventType.CollectionLoaded`               | `EventType.COLLECTION_LOADED`   |
| `Constant.Media.VideoMode.FullScreen`               | `VideoMode.FULLSCREEN`          |
| `Constant.Media.AudioMode.Normal`                   | `AudioMode.NORMAL`              |
| `Constant.Media.StartMode.AutoPlay`                 | `StartMode.AUTO_PLAY`           |
| `Constant.Media.InsertPosition.Top`                 | `InsertPosition.TOP`            |
| `Constant.Media.MetadataType.Video`                 | `MetadataType.VIDEO`            |
| `Constant.Media.MetadataFormat.Live`                | `MetadataFormat.LIVE`           |
| `Constant.GoBackOnlinePolicy.SEND_ALL`              | `GoBackOnlinePolicy.SEND_ALL`   |
| `Constant.Status.QUEUED`                            | `Status.QUEUED`                 |
| JSON key constants (e.g. `Constant.EVENT_TYPE_KEY`) | `PeachConstants.EVENT_TYPE_KEY` |

**Note:** The naming convention changed from PascalCase to SCREAMING_SNAKE_CASE for constants.

---

## Event Sending


### Collection Events

PeachCollector now uses `EventContext` objects for more flexibility:

```java
// Old
Event.sendCollectionDisplayed(
    "collection-id",
    itemsList,
    "app-section",
    "source",
    component,
    "experiment-id",
    "experiment-component"
);
```

```kotlin
// New
val context = EventContext.collectionContext().apply {
    appSectionID = "app-section"
    source = "source"
    component = myComponent
    experimentID = "experiment-id"
    experimentComponent = "experiment-component"
}
Event.sendCollectionDisplayed("collection-id", items = itemsList, context = context)
```

The same pattern applies to `sendCollectionLoaded`, `sendCollectionHit`, `sendCollectionItemDisplayed`, and all recommendation events.

### Recommendation Events

```java
// Old
Event.sendRecommendationHit("rec-id", "item-id", 3, "app-section", "source", component);
```

```kotlin
// New
val context = EventContext.recommendationContext().apply {
    appSectionID = "app-section"
    source = "source"
    component = myComponent
}
Event.sendRecommendationHit("rec-id", itemID = "item-id", hitIndex = 3, context = context)
```

---

## EventContext Factory Methods

Factory methods are simplified. Instead of passing all fields as parameters, you create the context and set fields individually:

```java
// Java
EventContext context = EventContext.mediaContext("media-id", "video", "app-section", "source", component);
```

```kotlin
// Kotlin
val context = EventContext.mediaContext("media-id", type = "video").apply {
    appSectionID = "app-section"
    source = "source"
    component = myComponent
}
```

---


## Summary of Breaking Changes

1. **Import paths** for constants changed (e.g., `Constant.EventType.MediaPlay` -> `EventType.MEDIA_PLAY`)
2. **Collection/Recommendation event methods** now take `EventContext` objects instead of flat parameters

---


