# FeatherW8 — Android music player

A Kotlin/Jetpack Compose Android music player for the realme GT 6T and Nothing Phone 3a. The interface uses true-black AMOLED surfaces and selectable accent colors. Nothing devices use NDot by default, with a Nothing-only disable switch; track, artist, and album metadata use the standard sans-serif face. Realme styling remains automatic, including its Mini Capsule behavior.

## Build

- See [`BUILD_LOCAL.md`](BUILD_LOCAL.md) for the complete local setup and build steps.
- The project uses Gradle Wrapper `9.4.1`, Android Gradle Plugin `9.2.0`, Kotlin Compose plugin `2.3.10`, SDK Platform 37, NDK `27.2.12479018`, and CMake `3.22.1`.
- Build the signed debug APK with `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`). Run JVM tests with `./gradlew :app:testDebugUnitTest`.
- The APK targets `arm64-v8a`, minimum Android 9 (API 28), target SDK 36.

## Product behavior

- Launches directly into Library. Bottom navigation is Library (left) and Playlists (right). The Library/Playlists pager follows the horizontal finger drag, progressively revealing the neighboring page; completed swipes settle on it, while partial drags spring back. List positions are preserved.
- Albums and Artists entry cards lead to artwork grids and track-list detail pages. Tapping a user or smart playlist opens its tracks first; playback starts only when a track or Play all is selected.
- Scans the phone's primary shared-storage volume only. It has no source picker and does not scan SD cards or USB-OTG.
- Settings retain only two filters: minimum file size from 0–1,500 KB in 50 KB steps, and minimum duration from 0–40 seconds in 5-second steps. The controls use the same slim progress track and round thumb as playback. Hi-Res/DSD inclusion is not user-configurable; the Full-play tracking row is removed.
- Accent color can be set to Crimson, Violet, Ice blue, Mint, Amber, or Rose. The Nothing-only NDot toggle is not shown on Realme.
- Library sorting is Track title, Date added, and Play count. Track rows show file size rather than the filename. Long-press any track in Library to enter multi-select mode, select all or cancel, then delete files, add them to a playlist or play queue, or share them. Android's deletion confirmation is retained for one or multiple files.
- The app includes Media3 playback, mini/full player, queue, favorites, user playlists, smart playlists, and automatic playback-progress tracking. A play is counted only after more than 90% of the track was actually heard; seeking does not count as played time. Playback pauses when Android reports a headset becoming disconnected/noisy, so it does not continue through the phone speaker. Long track titles repeat in a smooth marquee; tapping the title opens file information only on the full playback screen, not from Library or track lists.
- The full player slides down on Back and returns to the screen that opened it. Its Queue button opens the original app-level Queue screen; a left swipe shows a duplicate of the same queue UI inside the full player, and returning from that page reveals Now Playing again. Library and playlist scroll positions stay in memory while navigating; leaving Library for Playlists clears search. Two system-back presses exit from top-level screens.
- The library silently rescans on app entry/foreground; automatic scans stay quiet unless new track IDs are found. A manual scan started in Settings shows a toast with the number of newly found tracks, including zero.
- User playlists support rename and delete actions from a long-press; deleting a playlist leaves its audio files in the library.
- The mini-player is drawn as an overlay over the already-laid-out screen. Its vertical drag moves the card directly with the finger in either direction, including reversing direction without lifting; the underlying Scaffold is never resized during the gesture. Scrollable screens reserve a fixed bottom inset while a track is active so their last item can scroll clear of the card. The card fades during a downward pull, springs back below the dismissal threshold, and on a completed swipe stops playback, clears the queue, and dismisses. Tapping still opens the full player.
- The full player follows the finger in both vertical directions within one continuous drag, revealing the unchanged screen beneath it; the queue page remains opaque black. A partial pull springs back, while a completed swipe closes the player and leaves playback running.
- Player seeking uses a compact circular thumb, artwork fills the cover area, and the repeat control changes icons for repeat-all, repeat-one, and shuffle. Repeat and shuffle selections are saved and restored when the playback service is recreated. The album is shown only when metadata provides one. Format, file size, and bitrate appear below the controls; tapping the title opens file info with bitrate. Tapping the cover offers Change artwork, where an image can be chosen from storage.
- The app requests the supported display mode closest to 120 Hz at startup where available. On Nothing devices only, it reapplies that mode on resume and window focus to address the reported player/queue stutter; Realme's refresh-rate behavior is unchanged. Android and device power/display policies remain authoritative and may select a lower refresh rate.
- Scroll rendering keeps list filtering/sorting memoized; album art is sampled to display-sized bitmaps and held in a bounded cache. Playback progress is updated locally rather than polling/recomposing the whole app.
- Release version is `1.2.0` (`versionCode` 2). Nothing uses its system NDot typeface by default when available; the Settings switch can disable it on Nothing only. Track and artist names remain sans-serif. The Mini Capsule treatment is automatic and realme-only.

## DSD playback

DSF and uncompressed DSDIFF/DFF are routed through a custom Media3 extractor and the bundled LGPL-2.1 DSD-Nexus DSDPCM engine. DSF channel blocks are deblocked and bit-reversed; DSD is converted to 88.2 kHz, 16-bit PCM for ordinary Android audio output. The JNI library is bundled for `arm64-v8a`. The upstream license is included in `app/src/main/assets/third_party_licenses/DSD-Nexus-LGPL-2.1.txt`.

DST-compressed DSDIFF/DFF is not supported by this build. JVM tests cover DSF block parsing/bit order and uncompressed DSDIFF parsing; a host-native DSDPCM conversion smoke test also passed. No Android phone or emulator was attached during this build, so actual device playback, seeking, installation, and target-device behavior still need on-device verification.
