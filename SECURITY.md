# Security and privacy

## What FeatherW8 promises, and what it does not

FeatherW8 plays the audio files already on your phone and keeps a little data of its own about them: favorites, playlists, play counts and settings. The app itself **never sends any of it anywhere**:

- The app has **no internet permission** (check the APK: `aapt2 dump permissions`). The only network-related entry, `ACCESS_NETWORK_STATE`, comes from the Media3 library and can only tell whether a connection exists; it cannot send anything. There is no account, streaming, online cover lookup, analytics or crash reporting.
- Favorites, playlists, play counts, settings and the covers you picked are stored in the app's private SharedPreferences; cover thumbnails and the tags read from DSD files are kept in its private cache directory. Other apps cannot read them on a normal, unrooted phone. The app writes no files of its own to shared storage.
- The DSF and DSDIFF parsers are written in Kotlin and bounds-check every chunk size and offset; ID3 tags are read up to 32 MB. The native DSD-to-PCM engine (`libdsdpcm`) only receives whole frames whose size the JNI bridge checks, for 1 to 6 channels and DSD rates that are a multiple of 600 Hz.
- Deleting tracks asks for confirmation in the app and, on Android 11 and newer, again in Android's own dialog. *Set as ringtone* works only after you grant *Modify system settings* yourself.
- Releases are built by GitHub Actions and signed with the project key, which lives in repository secrets and never in the repo. Builds of pull requests from forks get the debug key.

What FeatherW8 does **not** do:

- Android backup is **enabled** (`android:allowBackup="true"`). If backup is turned on for the phone, favorites, playlists, play counts and settings may be copied to your Google account and restored with the app. The caches are not backed up.
- The stored data is **not encrypted** beyond what Android's file-based encryption of your phone already provides, and there is no app lock. Anyone who can unlock your phone can see what you listen to and how often.
- The playback service is **exported**, as Media3 requires. Any app on the phone can connect to the media session, see the title, artist, album and cover of what is playing and control playback, just like the notification, the lock screen and Bluetooth devices.
- Any app can hand FeatherW8 a file through *Open with*, and it starts playing right away. Formats other than DSD are parsed by Media3 and Android, not by FeatherW8's own code.
- There is no export or import. Uninstalling the app deletes favorites, playlists and play counts, and a build signed with a different key cannot be installed over a release without uninstalling it first.

## Reporting a problem

If you find a way for another app or a remote party to read your favorites, playlists or play counts, for the app to send data off the device, for a crafted DSF, DSDIFF or other audio file to corrupt memory or run code (a file that simply fails to play is an ordinary bug), for another app to delete files or change the ringtone through FeatherW8, or a weakness in how releases are built and signed, please report it **privately** through [GitHub security advisories](https://github.com/lemitz/FeatherW8/security/advisories/new) rather than a public issue. Bugs in Media3 or Android's own decoders belong upstream, and what is listed above under "does not" is known.
