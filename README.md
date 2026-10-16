# FeatherW8

[![build](https://github.com/lemitz/FeatherW8/actions/workflows/build.yml/badge.svg)](https://github.com/lemitz/FeatherW8/actions/workflows/build.yml)
[![release](https://img.shields.io/github/v/release/lemitz/FeatherW8?include_prereleases)](https://github.com/lemitz/FeatherW8/releases)
[![license](https://img.shields.io/github/license/lemitz/FeatherW8)](LICENSE)

A lightweight offline music player for Android, without accounts, ads, streaming or recommendations.
It scans the audio on your phone's internal storage, lets you browse it by track, album and artist,
and plays it through Media3. It also plays DSD files (DSF and DSDIFF) by converting them to PCM on the fly.

The interface is built with Jetpack Compose: true-black AMOLED surfaces, a choice of accent colors,
a mini player that you can swipe away and a full player that you can pull down. A bit of
device-specific polish (the Nothing NDot typeface, realme's Mini Capsule, a 120 Hz refresh-rate
request) is applied automatically on supported phones and does nothing elsewhere.

> Looking for the Windows terminal player? See [FeatherW8-CLI](https://github.com/lemitz/FeatherW8-CLI).

## Features

- **Library** with search, sorting by title, date added or play count, and albums/artists pages.
  Long-press to select several tracks, then add them to a playlist or the queue, share them or delete them.
- **Playlists:** your own playlists plus smart ones: recently added, Hi-Res, most played and favorites.
- **Honest play counts.** A track counts as played only after you actually listened to more than 90 % of it;
  seeking never adds listened time.
- **Player** with a queue, repeat-one/all, shuffle, file info (format, size, bitrate) and custom cover art
  picked from storage. Playback pauses when headphones are unplugged.
- **Filters** to hide tiny files: minimum size and minimum duration.
- **Fast lists:** covers are decoded off the main thread into a bounded memory cache and a disk cache that
  survives restarts; bitrates and metadata are cached too.
- **Six accent colors** on a true-black theme.

## DSD playback

DSF and uncompressed DSDIFF/DFF files play like any other track. Android has no DSD decoder, so
FeatherW8 brings its own:

1. **A custom Media3 extractor** ([`DsdExtractor.kt`](app/src/main/java/com/featherw8/player/audio/DsdExtractor.kt))
   sniffs and parses DSF and DSDIFF containers, de-interleaves DSF channel blocks, reverses their bit order,
   and exposes a seekable stream to ExoPlayer.
2. **A JNI bridge** ([`dsd_jni.cpp`](app/src/main/cpp/dsd_jni.cpp)) hands 1/75-second frames of DSD to
3. **libdsdpcm**, the DSD-to-PCM engine from [DSD-Nexus](https://github.com/wichers/dsd-nexus),
   which filters and decimates them to 88.2 kHz, 16-bit PCM that Android's normal audio output can play.

Seeking restarts the converter at the nearest frame so its filters start clean. The container parsers and
the DSF bit-order handling are covered by unit tests
([`DsdExtractorTest.kt`](app/src/test/java/com/featherw8/player/audio/DsdExtractorTest.kt)).

Limitations: DST-compressed DSDIFF is not supported, and output is PCM, not native DSD.

## Install

Download `FeatherW8-v*-arm64-v8a-debug.apk` from [Releases](https://github.com/lemitz/FeatherW8/releases)
and open it on your phone. These APKs are **debug-signed**, not signed with a release key, so Android will
ask you to allow installs from this source. Requirements: Android 9 (API 28) or newer, a 64-bit ARM
(`arm64-v8a`) device.

## Build

See [BUILD_LOCAL.md](BUILD_LOCAL.md) for the full setup. In short, with JDK 21, Android SDK 37, NDK
`27.2.12479018` and CMake `3.22.1` installed:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The same command runs on every push in [GitHub Actions](.github/workflows/build.yml). Pushing a `v*` tag
publishes the debug APK as a release.

## Third-party software

The DSD-to-PCM engine (`libdsdpcm` and `libsautil` from [DSD-Nexus](https://github.com/wichers/dsd-nexus))
is licensed under the **GNU LGPL v2.1**. Its source is vendored in this repository and built into
`libfeather_dsd.so`; the license text ships in the APK. Android and Kotlin libraries are Apache-2.0.
Details: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## License

FeatherW8's own code is released under the [MIT License](LICENSE).

## About AI

This project was developed with the help of AI coding assistants.
