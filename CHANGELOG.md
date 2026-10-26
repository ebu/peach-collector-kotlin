
# Change Log
All notable changes to this project will be documented in this file.

---
## [2.0.4] - 2026-10-26
### Fix
`PeachPlayerTracker` only sends `media_seek` for seeks within the current item ([#1](https://github.com/ebu/peach-collector-kotlin/issues/1)). It was sent for every position discontinuity reported by Media3, so also when:
- The playlist was changed (`setMediaItem`, `clearMediaItems`...), for the previous item and usually to 0 s
- The player moved to another item, automatically or with `seekToNextMediaItem()`
- Playback looped back to the start in repeat mode, a seek was adjusted, or silence was skipped

`PeachPlayerTracker` only sends `media_end` when playback reaches the end of the item, like on iOS. It was also sent, with `playback_position_s` 0, when the playlist was cleared or the current item removed, as the player ends then too. As on iOS, it isn't sent when the duration of the item is unknown

---
## [2.0.3] - 2026-10-22
### Fix
`PeachPlayerTracker` sends `media_play` once per playback, when playback actually starts:
- No second `media_play` when `play()` is called before the player is ready
- No extra `media_play` after a seek or buffering
- `media_play` is now sent when `trackMedia` is called while the player is already playing

### Improvement
- No `media_play` or `media_pause` is sent if playback never starts (paused while loading, or failed to load)
- No `media_pause` after `media_end`
- Heartbeats stop when the player is stopped or hits a playback error

---
## [2.0.2] - 2026-06-10
### Fix
`session_id` and `session_start_timestamp` are now properly saved on first launch
It fixes the possible multiple sessions within the inactivity interval

---
## [2.0.1] - 2026-04-07
### Improvement
Excluded useless media events like `media_seek` from 0s to 0s


---
## [2.0.0] - 2026-04-02
### Release
See [MIGRATION.md](MIGRATION.md) for a detailed guide on API changes when upgrading from `peach-collector-android`.

---