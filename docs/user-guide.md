# FeatherW8 user guide

FeatherW8 is an offline music player for Android. It plays the music that is on your phone, including DSD
files, and does nothing else: no accounts, no streaming, no ads, no network access. This guide describes
every screen and what it does. A [Russian version](user-guide.ru.md) is available; for how the app works
inside, see [architecture.md](architecture.md).

**Contents**
[Install and update](#install-and-update) ·
[First start](#first-start) ·
[Library](#library) ·
[Playlists](#playlists) ·
[Player](#player) ·
[Play queue](#play-queue) ·
[Play counts](#play-counts) ·
[DSD files](#dsd-files) ·
[Opening files from other apps](#opening-files-from-other-apps) ·
[Settings](#settings) ·
[Your data and privacy](#your-data-and-privacy) ·
[Troubleshooting](#troubleshooting)

---

## Install and update

FeatherW8 is not on Google Play. Releases are published as APK files on the
[Releases page](https://github.com/lemitz/FeatherW8/releases).

**Requirements:** Android 9 (API 28) or newer, and a 64-bit ARM phone (`arm64-v8a`), which is practically every
phone made in the last several years.

**Install**

1. Download `FeatherW8-v<version>-arm64-v8a.apk` from the latest release.
2. Open the file. Android will ask you to allow installing apps from the source you opened it with (your
   browser or file manager); allow it for that one time.
3. Confirm the installation.

**Update.** Download the newer APK and open it. Every release is signed with the same project key, so it
installs over the old one and keeps your favorites, playlists, play counts and settings.

**When an update will not install.** Android refuses to install an APK over an app that was signed with a
different key. That happens in two cases: you are moving from the very first release (1.2.0, which was signed
with a temporary debug key) to a later one, or you built the app yourself and are trying to put an official
release over it (or the other way round). Uninstall the app first and install again. Uninstalling removes
the app's data: favorites, playlists and play counts.

---

## First start

On first launch FeatherW8 asks for access to audio files. Without it the library stays empty ("Audio access
needed"); the "Scan Library" button on that screen asks again. The app then scans the phone's internal
storage and the Library tab fills with your tracks.

Permissions the app uses, and why:

| Permission | What for |
|---|---|
| Audio files (media) | Finding and playing your music. Required. |
| Notifications | The playback card in the notification shade and on the lock screen. Without it the music still plays, but you get no card. |
| "Modify system settings" | Only for **Set as ringtone**. Android sends you to its own settings screen to allow it the first time you use that action. |

The app does **not** ask for internet access and cannot reach the network.

---

## Library

![Library](screenshots/library.jpg)

The Library tab lists all tracks found on the phone.

### What is scanned

- Only the **phone's internal storage** is scanned. SD cards and USB drives are not.
- DSD files (`.dsf`, `.dff`, `.dsd`) are included even where Android's media index does not recognise them as
  audio.
- The scan runs by itself every time you open or return to the app, quietly; it only shows a message when it
  finds new tracks ("Found 3 new tracks"). A scan you start yourself in
  [Settings](#settings) always says how many new tracks it found, including "No new tracks found".

### Search

Type in the search field to filter the list. It matches the **title, artist, album and file format** of each
track, ignoring case. Opening the Playlists tab clears the search.

### Sorting

The sort button above the list offers three orders (the choice is remembered):

| Order | How it sorts |
|---|---|
| Track title | Alphabetically by title, ignoring case |
| Date added | Newest first |
| Play count | Most-played first (see [Play counts](#play-counts)) |

### Hidden tracks: the filters

Two filters in [Settings](#settings) hide clutter such as notification sounds and short clips:
**minimum file size** (0–1500 KB) and **minimum duration** (0–40 s). A track below either is not listed.
If a track you expect is missing, check these first. A track whose length is not known is never hidden by the
duration filter.

### A track row

Each row shows the cover, the title, "artist · duration", the file size and how many times you have
listened to it ("3×"). The title of the track that is playing is in the accent color.

- **Tap** a row to play it. The tracks you see, in the order you see them (with the current search, sort and
  filters), become the [play queue](#play-queue), starting from the track you tapped.
- **⋮ menu**:
  - *Add to playlist*
  - *Set as ringtone*
  - *Share* the file
  - *Delete file* (Android asks you to confirm; the file is removed from the phone)

### Selecting several tracks

**Long-press** a track to enter selection mode, then tap more tracks. The top shows "N selected" with
**Cancel** and **Select All / Deselect All** (this applies to the tracks currently listed). The bottom bar
offers:

| Button | Action |
|---|---|
| Delete | Deletes the selected files (Android asks you to confirm) |
| Add to Playlist | Adds them to a playlist you choose, or to a new one |
| Add to play queue | Appends them to the queue (tracks already in it are skipped) |
| Share file | Shares the files |

The Back button leaves selection mode.

---

## Playlists

![Playlists](screenshots/playlists.jpg)

The Playlists tab holds everything that groups tracks. Switch between Library and Playlists with the bottom bar
or by swiping sideways.

### Albums and Artists

The two cards at the top open a grid of albums and a grid of artists. Albums are grouped by album name and
artist; artists by name. Tap one to see its tracks; **Play all** starts from the first. These lists are built
from your library and cannot be edited.

### Smart playlists

Four playlists that fill themselves:

| Playlist | Contents |
|---|---|
| Recently added | The 10 newest tracks |
| Hi-Res | DSD files, and other files whose data rate is about 1.6 Mbit/s or more (estimated from file size and length, so lossless high-resolution audio qualifies) |
| Most played | The 10 tracks with the most [full plays](#play-counts) (tracks never fully played are not listed) |
| Favorites | Tracks you marked with the heart in the player |

Opening one shows its tracks; nothing plays until you tap a track or **Play all**.

### Your own playlists

- **New** (under "My playlists") creates a playlist. You can also create one from the picker that opens when
  you choose *Add to playlist* on a track.
- **Long-press** a playlist, or tap **Manage** inside it, to **rename** or **delete** it. Deleting a playlist
  never deletes the audio files.
- Inside a playlist, **hold a track and drag** it to change the order, or tap the **cross** on its right to
  remove it from the playlist (the file is untouched). Changes are saved immediately.
- Tapping a track plays the playlist from that track, in the playlist's order.

---

## Player

![Player](screenshots/player.jpg)

### Mini player

While something is loaded, a small card sits above the bottom bar: cover, title, artist, a play/pause button
and a thin progress line.

- **Tap** it to open the full player.
- **Drag it down** to stop the music and dismiss it (this also clears the queue).

### Full player

| Control | What it does |
|---|---|
| ← (top left) | Closes the player; the music keeps playing. Dragging the whole screen down does the same. |
| ⋮ (top right) | The track menu: *Add to playlist*, *Track info*, *Set as ringtone*, *Share*, *Delete file* |
| Cover | Tap to change the cover: choose an image from your storage, or go back to the original |
| Title | Tap for *Track info* (artist, album, file, format, bit rate, duration, size, full plays) |
| ♥ | Adds or removes the track from Favorites |
| Progress bar | Drag or tap to seek |
| Playback mode (left button) | Tap to cycle **Repeat off → Repeat all → Repeat track → Shuffle → Repeat off**; long-press for a menu of the four modes. The choice is remembered. |
| ⏮ ⏯ ⏭ | Previous track, play/pause, next track |
| Queue button (right) | Opens the [play queue](#play-queue); swiping left on the player does the same |

Below the controls a line shows the file's format, size and bit rate; for DSD files it also names the DSD rate
(for example "DSF · DSD128 · 344.6 MB · 11290 kbps").

The album name appears under the artist only when the file carries one.

### Notification and lock screen

Playback continues with the screen off and in other apps. The notification card has play/pause and skip
controls; **tapping the card opens the player**. Music pauses by itself when headphones are unplugged, so it
does not suddenly continue through the speaker.

---

## Play queue

![Queue](screenshots/queue.jpg)

The queue is the list of tracks that will play, in order. It is created whenever you start playing from a list:
the Library (as currently searched, sorted and filtered), a playlist, an album or an artist.

Open it with the queue button in the player (or swipe left on the player).

- It **opens at the track that is playing**, with a couple of tracks above for context, and that track is
  highlighted (also while paused). If you keep the queue open and the player moves on, the list follows.
- **Tap** a track to jump to it.
- **Hold and drag** a track to move it. The list scrolls by itself when you hold a track near the top or bottom
  edge.
- Tap the **cross** on a track to remove it from the queue. Removing the track that is playing continues with
  the next one; removing the last remaining track closes the player.
- **Clear** (top right) removes every other track from the queue. The current track is kept but starts again
  from the beginning.
- **Back to player** at the bottom (or the Back button) returns to the player.
- Use **Add to play queue** in the Library's selection mode to append tracks to the end.

---

## Play counts

The "3×" on a track, **Full plays** in *Track info*, the *Most played* playlist and the *Play count* sort all
use one number: how many times you have **really listened** to a track.

A play is counted when you have heard **more than 90 %** of the track. Only time actually played counts:

- paused time does not count;
- **seeking does not count**: skipping to the end does not add a play, and neither does jumping around;
- what you listened to before a seek still counts toward the 90 %.

A track is counted once per play; with *Repeat track* every completed loop is another play. Counts are kept
on the phone and per track.

---

## DSD files

FeatherW8 plays DSD audio: **DSF** files and **uncompressed DSDIFF** (`.dff`) files, including DSD64, DSD128,
DSD256 and higher, mono to six channels.

Android has no DSD decoder, so the app converts DSD to ordinary PCM while playing (16-bit, 88.2 kHz) and
sends that to the phone's normal audio output. Two things follow from this:

- It is **not** native DSD output (there is no DoP), so a DSD-capable DAC receives PCM.
- **DST-compressed** DSDIFF is not supported.

DSD files look like any other track. Title, artist, album and cover art are read from the file's own tags;
duration and bit rate come from its header; the *Hi-Res* playlist includes them. The bit rate shown is the
exact DSD data rate (sample rate × channels), not an estimate.

---

## Opening files from other apps

FeatherW8 is offered in "Open with" for audio files and for `.dsf`, `.dff` and `.dsd` files in file
managers, messengers and the like. Opening a file starts playing it in FeatherW8. If the file is already in
your library it is the same track (with its play count and cover); otherwise it plays as a standalone track.

---

## Settings

![Settings](screenshots/settings.jpg)

Open Settings with the gear at the top right of the Library and Playlists tabs.

| Section | What you can do |
|---|---|
| **Library filters** | Set the *minimum file size* (0–1500 KB, in steps of 50) and the *minimum duration* (0–40 s, in steps of 5). Tracks below either are hidden from the Library. |
| **Accent color** | Pick one of six: Crimson, Violet, Ice blue, Mint, Amber, Rose. Used for highlights throughout the app. |
| **Library scan** | *Scan Library* searches internal storage now and tells you how many new tracks it found. |
| **System** | On a Nothing phone: a switch for the dotted *NDot* typeface (on by default). On a realme phone: a note that Mini Capsule works automatically. Elsewhere: "System default". |

The app also asks the screen for a refresh rate close to 120 Hz where the phone offers one; the system has the
last word.

### Back button

Back closes things from the top down: the player, then selection mode, then any detail screen (settings, an
album, a playlist). On the Library or Playlists tab, press Back twice within two seconds to exit.

---

## Your data and privacy

- **No internet permission.** The app cannot send anything anywhere and has no analytics.
- **What is stored on the phone:** your favorites, playlists, play counts, sort order, filters, accent color,
  playback mode, custom covers you chose, and some caches (below). It is all in the app's private storage.
- **Caches** (safe to clear from Android's app settings; they are rebuilt): a thumbnail cache of covers
  (up to about 96 MB), and the tags and covers read from DSD files.
- **Tracks are identified by the id Android's media index gives them.** If Android re-indexes a file under a
  new id (it can happen when a file is moved or restored), the app sees a new track and the old track's
  favorite, playlist entries and play count no longer apply to it.
- **Backups.** The app allows Android's automatic backup, so on a phone set up with a Google backup its
  settings may be restored with the app.
- **Uninstalling removes all of the app's data.**

---

## Troubleshooting

**The Library is empty.** Check that you allowed access to audio files (Android Settings → Apps → FeatherW8
→ Permissions), then run *Scan Library* in Settings. Only internal storage is scanned; music on an SD card is
not found.

**A track is missing.** Lower or zero the minimum size and duration filters in Settings. Make sure the search
field is empty. If the file was added moments ago, run *Scan Library*.

**A DSD file does not appear or will not play.** Only DSF and uncompressed DSDIFF play; DST-compressed DSDIFF
cannot be played. Make sure audio access is allowed, then run *Scan Library*.

**Play counts do not go up.** A play needs more than 90 % of the track actually played, and seeking does not
count. Some phones slow apps down heavily with the screen off, which older versions of the app handled badly;
update to 1.4.1 or newer. If it still does not count, allow the app to run in the background:

- Android Settings → Apps → FeatherW8 → Battery → **Unrestricted** (the wording differs by phone);
- on Nothing OS also switch off *Sleep standby optimization* in the battery settings.

**Music stops in the background or the card disappears.** The same battery settings apply: set FeatherW8 to
unrestricted battery use and, on Nothing OS, turn off *Sleep standby optimization*.

**The notification card does nothing when tapped.** Update to 1.3.1 or newer.

**An update will not install.** See [When an update will not install](#install-and-update): uninstall first (this
removes the app's data).

**No notification card.** Allow notifications for FeatherW8 in Android's app settings.

**Set as ringtone sends me to a settings screen.** That is Android asking you to allow FeatherW8 to
"modify system settings", which setting a ringtone requires. Allow it, go back, and try again.

**Found a bug?** Open an issue at <https://github.com/lemitz/FeatherW8/issues> and say which version and phone
you have.
