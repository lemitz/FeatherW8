# Changelog

All notable changes to FeatherW8, newest first. Each release is on the
[Releases page](https://github.com/lemitz/FeatherW8/releases) with its APK; the text below is the same as the
release notes in [`docs/release-notes/`](docs/release-notes).

The project uses `MAJOR.MINOR.PATCH` versions; see [Versioning](docs/development.md#versioning).

## 1.4.1 — 2026-10-19

- The play counter is now reliable on phones that slow an app down while the screen is off (Nothing OS in
  particular). It used to give up on any stretch where the app was not polled for more than 1.5 seconds, so a
  track listened to with the screen off could play to the end and still never be counted. Listening is now
  measured from the player's position when it pauses, is seeked or ends, no matter how often the app was woken
  up in between.
- The player now holds a wake lock while playing, so the screen-off case stays steady in the first place.
- Files whose length the player cannot work out (VBR MP3 without a length header, for example) are counted
  too; the length found in the library is used for them.

## 1.4.0 — 2026-10-19

- You can now reorder the play queue: hold a track and drag it to where it should play. A small round button
  with a cross takes a track out of the queue (the track that is playing is replaced by the next one).
- The same in your own playlists: hold and drag to reorder, the cross removes a track from the playlist. Both
  are saved right away. (Albums, artists and the smart playlists are built from the library, so they stay as
  they are.)
- The ⋮ menu is gone from the queue and from your own playlists: its library actions (add to playlist,
  ringtone, share, delete file) did nothing in the queue. They are still in the library and in the player.
- Playlists, albums and artists no longer show a meaningless "0×" play counter on every track.

## 1.3.1 — 2026-10-16

- The playback queue now opens at the track that is playing instead of at the top, with a couple of tracks
  above it for context, and keeps following the player when it moves on to the next track.
- The current track is marked in the queue (tinted row, accent title), also while playback is paused; before,
  the mark disappeared as soon as you paused.
- The queue no longer shows a meaningless "0×" play counter on every row.
- Tapping the player card in the notification shade (or on the lock screen) now opens the player; on some
  systems, Nothing OS among them, the card did nothing.

## 1.3.0 — 2026-10-16

- DSD files show up like any other track: real title, artist, album and cover from their tags, the true
  duration, and the exact bit rate with the DSD rate (DSD64/128/256).
- "Open with": FeatherW8 is offered for audio files and for `.dsf` / `.dff` / `.dsd` in file managers, and
  plays what you opened.
- Faster: this is now an optimised (R8) release build instead of the debug build, and switching between
  Library and Playlists no longer stutters when you tap quickly.
- DSD tracks no longer disappear from Library when a minimum-duration filter is set.
- Updates now install over each other: the APK is signed with the project's own key. **1.3.0 was the
  exception:** the older debug-signed build had to be uninstalled first (its favorites, playlists and play
  counts were not carried over).

## 1.2.0 — 2026-10-16

First published release (debug-signed).

- DSD playback: DSF and uncompressed DSDIFF/DFF via a custom Media3 extractor and a bundled DSD-to-PCM engine
  (88.2 kHz / 16-bit output).
- Library, albums, artists, user and smart playlists, favorites, and play counts that only count tracks you
  actually heard.
- Mini player and full player with queue, repeat/shuffle, and custom cover art.
- Faster scrolling: bounded in-memory and on-disk cover caches, cached track metadata.
- Selectable accent colors and library size/duration filters.
