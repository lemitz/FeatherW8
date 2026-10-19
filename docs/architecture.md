# How FeatherW8 works

This document describes how the app is put together: what runs where, how playback, the library, DSD
decoding, play counting and the caches work, and what is stored on the device. It is meant for someone who
wants to change the code. For the user's view see the [user guide](user-guide.md); for building and releasing
see [development.md](development.md). A [Russian version](architecture.ru.md) is available.

**Contents**
[Overview](#overview) ·
[Source layout](#source-layout) ·
[Processes and threads](#processes-and-threads) ·
[UI and state](#ui-and-state) ·
[Playback](#playback) ·
[Play counting](#play-counting) ·
[The library](#the-library) ·
[DSD pipeline](#dsd-pipeline) ·
[Artwork](#artwork) ·
[Reordering lists](#reordering-lists) ·
[Intents from outside](#intents-from-outside) ·
[Stored data](#stored-data) ·
[Testing](#testing) ·
[Known limitations](#known-limitations)

---

## Overview

```
 ┌───────────────────────────── app process ──────────────────────────────┐
 │                                                                         │
 │  MainActivity ── Compose UI (ui/FeatherW8App and screens)               │
 │       │  MediaController (Media3)                                       │
 │       ▼                                                                 │
 │  PlaybackService ── MediaSession ── ExoPlayer                           │
 │       │                                  │                              │
 │       │ play counter (ActualPlayCounter) │ DsdExtractor ─ JNI ─ libdsdpcm
 │       ▼                                  ▼                              │
 │  PlayCountStore (SharedPreferences)   audio output                      │
 │                                                                         │
 │  MediaLibraryRepository ── MediaStore ──► List<MusicTrack>              │
 │       └── DsdLibrary (tags, covers, durations of DSD files, cached)     │
 └─────────────────────────────────────────────────────────────────────────┘
```

Everything lives in one process. The UI never touches the player directly: it talks to it through a Media3
`MediaController`, and the player itself lives in a `MediaSessionService`, so playback survives the activity
being closed and the notification, lock screen and headset buttons control the same player.

Kotlin, Jetpack Compose (Material 3), AndroidX Media3 (ExoPlayer, Session, Extractor), kotlinx.coroutines, and
a small C++ library (CMake / NDK) for DSD-to-PCM conversion.

---

## Source layout

`app/src/main/java/com/featherw8/player/`

| Path | Responsibility |
|---|---|
| `MainActivity.kt` | The single activity: theme, 120 Hz request, turning incoming intents into UI state |
| `playback/PlaybackService.kt` | `MediaSessionService`: the ExoPlayer, the session, repeat/shuffle persistence, play counting |
| `audio/DsdExtractor.kt` | Media3 `Extractor` for DSF and DSDIFF: parsing, seeking, feeding converted PCM |
| `audio/NativeDsdDecoder.kt` | JNI wrapper around the native converter |
| `audio/DsdMetadata.kt` | Reads stream parameters and ID3 tags/cover straight from a DSD file |
| `audio/DsdLibrary.kt` | Caches what `DsdMetadata` read; applies it to a `MusicTrack` |
| `data/MediaLibraryRepository.kt` | Scans MediaStore into `MusicTrack`s |
| `data/MusicTrack.kt`, `TrackFormat.kt` | The track model with precomputed labels; duration/size formatting |
| `data/ActualPlayCounter.kt` | The "really listened to 90 %" logic |
| `data/PlayCountStore.kt`, `TrackInfoStore.kt` | Persisted play counts and measured bit rates |
| `data/CoverDiskCache.kt` | JPEG thumbnail cache on disk |
| `ui/FeatherW8App.kt` | The root composable: all app state, navigation, wiring of everything below |
| `ui/theme/` | Colors, accent options, device profile (Nothing / realme / generic), `FeatherW8Theme` |
| `ui/model/` | UI types (`AppPage`, `SortMode`, collections, playlists), list helpers (`moved`, `movedOver`) |
| `ui/screens/` | Library, Playlists, Albums/Artists/detail, Queue, Settings |
| `ui/player/` | Full player, mini player, the overlay that hosts both |
| `ui/components/` | Track row, dialogs, the reorder machinery |
| `ui/artwork/` | Cover loading, caches, the background warmer, user-chosen covers |
| `ui/actions/` | Share, ringtone, delete, mapping a file from another app to a track, `MusicTrack → MediaItem` |

Native code is in `app/src/main/cpp/`: `dsd_jni.cpp` (the JNI bridge), `external/libdsdpcm/` (bindings) and
`third_party/dsd-nexus/` (the vendored LGPL-2.1 engine and utility library).

---

## Processes and threads

One process, with these threads that matter:

| Work | Where |
|---|---|
| UI, Compose, `MediaController` callbacks, `PlaybackService` callbacks and the play-count poll | main thread |
| Library scan (MediaStore queries, reading DSD headers and tags) | `Dispatchers.IO` |
| Filtering, sorting, grouping albums/artists, building smart playlists | `Dispatchers.Default` |
| Cover decoding (at most 3 at a time), cover warming (one at a time) | `Dispatchers.IO` |
| Decoding audio, including DSD extraction and conversion | ExoPlayer's own loading and playback threads |
| Reading play counts and building the media items for a new queue | `Dispatchers.IO` / `Default` |

---

## UI and state

There is one activity, `MainActivity`, and no navigation library. `ui/FeatherW8App.kt` is the root composable;
it owns essentially all the state in local `remember` / `rememberSaveable` variables and passes plain values and
callbacks down to stateless screens.

**Pages.** `AppPage` (in `ui/model/UiModels.kt`) lists the destinations: `LIBRARY`, `PLAYLISTS`, `SETTINGS`,
`ALBUMS`, `ARTISTS`, `ALBUM_DETAIL`, `ARTIST_DETAIL`, `PLAYLIST_DETAIL` (and a legacy `QUEUE` that is mapped to
`LIBRARY` on start). The current page is `rememberSaveable`. The full player is not a page: it is an overlay
(`PlayerOverlays`) shown while `playerOpen` is true, and inside it a two-page pager holds the player (page 0) and
the queue (page 1).

**Library and Playlists tabs** are two pages of a `HorizontalPager`. The selected tab and the pager have to
agree whichever one the user touches, which is subtle:

- tapping a tab sets `page`; an effect then animates the pager to it, comparing with the pager's **target** page
  (not the last settled one), so a quick tap back while it is moving reverses the animation;
- swiping settles the pager, which sets `page` back;
- the neighbouring tab is kept composed (`beyondViewportPageCount = 1`) so switching never builds a page in the
  middle of the animation.

**Derived state** is computed off the main thread and published back: the visible track list (filters, search,
sort), albums and artists, smart playlists, play counts. When a rescan produces a list equal to the current one
it is not assigned, so a return to the app does not re-run all of that.

**Shared state vs. the player.** The play queue the UI shows is a `List<MusicTrack>` held by `FeatherW8App`
that mirrors the player's playlist (see [Known limitations](#known-limitations)).

**Style.** The theme is true black with a selectable accent (`LocalFeatherAccent`). On a Nothing phone the
`NDot55` system typeface is used by default; `DeviceProfile` is detected from `Build` strings.

---

## Playback

`PlaybackService` creates an `ExoPlayer` and a `MediaSession`:

- **Extractors.** The default extractors plus `DsdExtractor` first in the list, so DSD files are claimed by it
  (it sniffs the `DSD ` / `FRM8` signature).
- **Audio.** `AudioAttributes.DEFAULT` with audio focus handling on; `setHandleAudioBecomingNoisy(true)` pauses
  when headphones are unplugged.
- **Wake lock.** `setWakeMode(C.WAKE_MODE_LOCAL)` holds a partial wake lock while playing, so the CPU does not
  sleep between audio buffers with the screen off.
- **Repeat and shuffle** are saved to SharedPreferences on every change and restored when the service is
  created. The single player button cycles Repeat off → all → track → Shuffle (shuffle is exposed as a fourth
  "mode"; it is stored as repeat off + shuffle on).
- **Session activity.** `setSessionActivity` points at `MainActivity` with the `ACTION_OPEN_PLAYER` action, so a
  tap on the notification card opens the app and the full player (see
  [Intents from outside](#intents-from-outside)).

**Starting playback.** `playTracks(items, index)` in `FeatherW8App` sets the UI's queue, shows the player
immediately with the chosen track, and builds the `MediaItem`s on `Dispatchers.Default` before handing them to
the controller (`setMediaItems`, `prepare`, `play`), so the slide-in animation does not stall. A `MediaItem`
carries the track's id as `mediaId`, its URI, title/artist/album, artwork URI (a user-chosen cover wins over the
media index's) and the duration found by the library.

**Editing the queue.** `moveQueueTrack` and `removeQueueTrack` apply the change to the controller
(`moveMediaItem`, `removeMediaItem`) and to the UI's list together. Removing the last track dismisses the
player, which also stops and clears it.

**Mini player.** `MiniPlayerBar` polls the controller's position every 250 ms while visible and playing (the
state is local to the bar, so the rest of the screen does not recompose); a vertical drag follows the finger and
dismisses past a threshold.

---

## Play counting

A track counts as played when more than 90 % of it was **actually listened to**. The logic is in
`data/ActualPlayCounter.kt`, driven by `PlaybackService`; the count is persisted by `PlayCountStore`.

The counter works in **segments**: from the moment the player starts, resumes, or lands from a seek, to the
moment it pauses, is seeked or the track ends. The value of a segment is the player's position when the segment
is closed minus where it began, so it does not depend on how often the counter is asked. (An earlier version
summed small steps between polls and discarded any step over 1.5 s; on a phone that throttles apps with the
screen off that threw almost all the time away.)

Two safeguards:

- a **seek** closes the segment before the jump and opens one after it, so the time jumped over is never
  credited, and what was played before the seek is;
- a segment can never be worth more than the **clock** says has elapsed since it began (plus a 750 ms
  tolerance), so a position jump that was not reported is not credited either.

The service closes segments on the player's own events instead of waiting for the next poll:
`onIsPlayingChanged` (pause/resume), `onPositionDiscontinuity` with a seek, and a track change or automatic
transition (which finishes the old track at its end position and starts a new counter). A poll every 250 ms
still runs, so a play is recorded the moment 90 % is passed rather than at the end of the track.

The duration used for the 90 % rule is the player's, or, if the player cannot tell (VBR MP3 without a length
header), the library's duration carried on the `MediaItem`. A track without any known duration is never counted.

---

## The library

`MediaLibraryRepository.scanInternalStorage()` builds the list of `MusicTrack`s:

1. Queries `MediaStore.Audio` on the **primary external volume** only, newest first.
2. Queries `MediaStore.Files` for names ending `.dsf`, `.dff` or `.dsd`, because the media index often does not
   classify DSD files as audio; rows already found in step 1 are skipped.
3. For every DSD track, replaces what MediaStore guessed (it reports the folder name as the album, no artist and
   no duration) with what the file says: see [DSD pipeline](#dsd-pipeline).
4. Sorts by date added and **precomputes** the labels the UI shows over and over (extension, format label,
   subtitle, size label, artwork URI), on the scanning thread rather than in composables.

A scan runs when the app is entered (`onStart`) and on request from Settings. The IDs of all tracks seen are
kept so the next scan can tell how many are new.

**Identity.** A track's id is MediaStore's `_ID`, as a string. Favorites, playlists, play counts, cached
bit rates and custom covers are all keyed by it.

**Grouping** (`ui/model/Collections.kt`): albums by `albumId|artist|album` (or `artist|album` when the album id
is unknown, as for DSD), artists by name; both sorted case-insensitively.

**Smart playlists:** *Recently added* = 10 newest; *Hi-Res* = DSD, or a data rate of at least 200 000 bytes per
second estimated from size and duration; *Most played* = up to 10 with at least one full play; *Favorites*.

**Filters.** Minimum size and duration are applied to the Library list only. A track whose duration is 0 (unknown)
passes the duration filter.

---

## DSD pipeline

```
DSF / DSDIFF file
   └─ DsdExtractor            sniff, parse the container, expose a seekable stream
       └─ DsdDataReader       de-interleave and bit-reverse DSF blocks; DSDIFF is already interleaved
           └─ NativeDsdDecoder → dsd_jni.cpp → libdsdpcm
               1/75-second frames of DSD in, 16-bit PCM at 88.2 kHz out
```

**Container parsing** (`DsdFileInfo`). DSF: the `DSD ` chunk, `fmt ` (channels, rate, sample count, block
size), then `data`. DSDIFF: the `FRM8` form, `PROP` (`FS`, `CHNL`, `CMPR`), then the `DSD ` chunk;
DST-compressed files are rejected. Channels must be 1–6 and the rate a multiple of 600 Hz.

**Extraction.** Each call to `read` fills one input frame, 1/75 s of interleaved DSD (for stereo DSD64 that is
9 408 bytes), converts it, and emits the resulting 1/75 s of 16-bit PCM as one sample (4 704 bytes for stereo).
A last incomplete frame is padded with the DSD silence pattern `0x69`. DSF stores each channel in separate
blocks with the least significant bit first, so blocks are de-interleaved and each byte is bit-reversed before
conversion.

**Seeking.** `DsdSeekMap` maps a time to a byte position in the file (block-aligned for DSF). Because the
converter's filters carry state, a seek closes the decoder and opens a new one, then positions the reader at the
right frame.

**Native converter** (`dsd_jni.cpp`). A handle holds a `libdsdpcm` converter set up with
`dsdpcm_init_multistage` (75 frames/s, the DSD rate, 88 200 Hz output, 64-bit floating point). `nativeConvert`
runs one frame and quantises the doubles to 16-bit with clamping. The library is built from `OBJECT` and `STATIC`
CMake targets that are linked into a single `libfeather_dsd.so` together with the JNI code; this is described in
[THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md). The engine is LGPL-2.1; its source is vendored in the
repository.

**Tags, cover and duration.** MediaStore knows none of them for DSD, so `DsdMetadataReader` reads them from the
file with a random-access descriptor: DSF keeps an ID3v2 tag at an offset stored in its header (bytes 20–27),
DSDIFF has an `ID3 ` chunk (with `DIIN` title/artist chunks as a fallback). The ID3 data is decoded with
Media3's `Id3Decoder` (title `TIT2`, artist `TPE1`/`TPE2`, album `TALB`, picture `APIC`, preferring the front
cover). The exact bit rate is `sample rate × channels`.

`DsdLibrary` caches the result per file in `cacheDir/dsd-meta/`, keyed by a hash of `"id:size"`: a JSON file with
the values and, if there is a cover, a JPEG (shrunk to at most 1 024 px; kept as is when already small).
A file that cannot be read is remembered as such. `MusicTrack.withDsdInfo` applies it: title, artist, album,
duration, `albumId = -1` (so grouping uses the real tags instead of the folder), cover URI, exact bit rate and a
"DSD128 · stereo" detail string.

---

## Artwork

- **Memory:** an `LruCache` of 24 MB, keyed by `uri@pixels`.
- **Decoding:** at most 3 at a time (a semaphore), on `Dispatchers.IO`, cancelled when the row scrolls away.
  Thumbnails are 192 px, the full-screen cover 720 px. On Android 10+ `ContentResolver.loadThumbnail` is tried
  first; otherwise the image is decoded with a sample size.
- **Disk:** thumbnails (≤ 512 px requests) are kept as JPEG (quality 85) in `cacheDir/covers/`, named by a hash
  of the key, written to a temporary file and renamed. The directory is trimmed to 96 MB, oldest first.
- **Warmer:** `CoverWarmer` generates missing thumbnails in the background, one at a time, pausing while the
  list scrolls.
- **Which image:** a cover the user chose (`CustomArtworkStore`, stored as a URI with persisted read permission)
  wins; then a cover embedded in a DSD file (`MusicTrack.coverUri`); then the media index's album art.

---

## Reordering lists

Compose has no built-in drag-to-reorder for lazy lists, so `ui/components/Reorderable.kt` implements it. It is
used by the queue and by playlists the user made.

`ReorderState` tracks the held row (by key) and where the finger has put its top edge in the list's viewport.
The held row is not moved in the list while the finger travels: it is drawn at the finger with a translation
(`reorderLayer`), measured from wherever the list currently has it, so the row stays under the finger when the
list reorders beneath it. When the held row's centre crosses another reorderable row, `onMove(from, to)` is
called and the list is expected to reorder. A move that has been asked for but not yet laid out is not asked for
twice. While a row is held near the top or bottom edge, a coroutine scrolls the list by a fixed step every frame.

The gesture (`reorderGesture`, long-press then drag) is attached **inside** the row's click handling so that it
sees the touch first and consumes it; holding a row and letting go therefore does not also count as a tap.

Positions passed to `onMove` count only the reorderable rows (a header offset and a row count are given). The
queue applies them to the player and its list; playlists apply them **by track id** (`movedOver`), because the
list on screen skips tracks whose file is gone, so a position on screen is not a position in the stored playlist.

---

## Intents from outside

`MainActivity` is `singleTask`, so a running app gets new intents in `onNewIntent` instead of a second copy.

- **Open with.** Two `VIEW` filters in the manifest: any `audio/*` (and `application/ogg`), and `content`/`file`
  URIs whose path ends in `.dsf`, `.dff` or `.dsd` (a file manager often reports DSD as a generic type; Android's
  `pathPattern` needs one pattern per number of dots in the path, hence the repetition). The URI becomes UI
  state; once the player service is connected, `resolveExternalTrack` maps it to a library track (by URI or
  MediaStore id) or builds a standalone track (id `ext:<uri>`, name and size from the provider, DSD tags
  via `DsdLibrary`), and plays it.
- **Open the player.** The session activity sends `ACTION_OPEN_PLAYER`. The request is held until the playing
  track is known (after the library scan on a cold start) and then opens the full player; if nothing is playing
  it is dropped.
- Both are ignored when the activity is recreated (`savedInstanceState != null`) or restored from history, so a
  rotation does not replay them.

---

## Stored data

All of it is private to the app.

**SharedPreferences `featherw8`**

| Key | Contents |
|---|---|
| `favorites` | Set of track ids |
| `user_playlists` | JSON array of `{id, name, trackIds}` |
| `sort_mode` | `TITLE`, `ADDED` or `PLAYS` |
| `minimum_size_kb`, `minimum_duration_seconds` | Library filters (the legacy `minimum_size_mb` is migrated) |
| `accent_color` | Accent id (`red`, `violet`, `cyan`, `mint`, `amber`, `rose`) |
| `nothing_dot_font_enabled` | NDot switch (Nothing phones) |
| `playback_repeat_mode`, `playback_shuffle_enabled` | Restored by the service |
| `known_track_ids` | Ids seen by the last scan, to count new tracks |
| `custom_cover_uri_<id>` | A cover chosen by the user |

**SharedPreferences `featherw8_play_counts`:** `full_plays_<id>` → integer.

**SharedPreferences `featherw8_track_info`:** `bitrate_<id>` → `[~]bitsPerSecond|sizeBytes` (`~` marks an
estimate; the size makes a replaced file be measured again).

**Cache directory:** `covers/` (thumbnails) and `dsd-meta/` (DSD tags and covers). The system may delete them;
they are rebuilt.

---

## Testing

JVM unit tests (`app/src/test`), run by `./gradlew :app:testDebugUnitTest` and by CI:

| Test | Covers |
|---|---|
| `DsdExtractorTest` | DSF block de-interleaving and bit order; uncompressed DSDIFF parsing |
| `DsdMetadataReaderTest` | Stream parameters, ID3 title/artist/album/cover (ASCII and UTF-16) for DSF and DSDIFF, `DIIN` fallback, the DSD rate label |
| `ActualPlayCounterTest` | The 90 % rule, rare polling, unreported jumps, seeks, late pause, one count per track |
| `ListEditsTest` | `moved` / `movedOver` including ids that are not neighbours in storage |

Nothing here is an instrumented (on-device) test: the UI, the player service and the JNI converter are checked by
hand on a device.

A note for writing tests that touch Media3: `ParsableByteArray` probes the Android framework to see if it is
running in a test and fails to initialise on a plain JVM. The metadata tests set
`ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(false)` first, which skips that probe.

---

## Known limitations

- **The on-screen queue is not rebuilt from the player.** The queue is a list held by the UI; the player has its
  own. If the activity is recreated while music keeps playing (the app removed from recents and reopened from the
  notification, or a rotation), the queue screen starts empty even though the player still has its tracks,
  until you start playing from a list again. The current track itself is still found through the library.
- **No ViewModel; one large root composable.** `FeatherW8App.kt` (about 1 100 lines) holds the state and the
  logic. Parts that are only presentation have been extracted (dialogs, the player overlays, screens), but
  the state itself has not, so most of it is `remember`-local and only some survives process death.
- **Ids come from MediaStore.** A file re-indexed under a new id looks like a new track.
- **DSD output is PCM**, not native DSD, and DST-compressed DSDIFF is not supported.
- **Only the primary internal volume is scanned.**
- **No on-device tests;** see above.
