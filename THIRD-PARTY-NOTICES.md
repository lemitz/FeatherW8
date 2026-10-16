# Third-party notices

FeatherW8's own code is released under the [MIT License](LICENSE). It bundles and
depends on the third-party components below, which keep their own licenses.

## DSD-Nexus (libdsdpcm, libsautil) — LGPL-2.1

The DSD-to-PCM engine used for DSF/DSDIFF playback is taken from
[DSD-Nexus](https://github.com/wichers/dsd-nexus) (by wichers) and licensed under the **GNU Lesser General Public
License, version 2.1**.

- Full license text: [`app/src/main/assets/third_party_licenses/DSD-Nexus-LGPL-2.1.txt`](app/src/main/assets/third_party_licenses/DSD-Nexus-LGPL-2.1.txt)
  (also shipped inside the APK) and
  [`app/src/main/cpp/third_party/dsd-nexus/LICENSE`](app/src/main/cpp/third_party/dsd-nexus/LICENSE).
- Only the library subset FeatherW8 needs (`libdsdpcm`, `libsautil`) is vendored in
  [`app/src/main/cpp/third_party/dsd-nexus/`](app/src/main/cpp/third_party/dsd-nexus/);
  the rest of DSD-Nexus (including the GPL-licensed Nexus Forge GUI) is not included.
  The C++ glue used by FeatherW8 lives in
  [`app/src/main/cpp/external/libdsdpcm/`](app/src/main/cpp/external/libdsdpcm/) and
  [`app/src/main/cpp/dsd_jni.cpp`](app/src/main/cpp/dsd_jni.cpp).
- Upstream components and their origins:
  - `libdsdpcm` — derived from the SACD Decoder plugin,
    Copyright (c) 2011–2023 Maxim V. Anisiutkin, LGPL-2.1-or-later.
  - `libsautil` — derived from FFmpeg's `libavutil`,
    Copyright (c) Fabrice Bellard and the FFmpeg developers, LGPL-2.1-or-later.

### How the library is linked

`libdsdpcm` and `libsautil` are built as CMake `OBJECT`/`STATIC` libraries and are
linked into a single `libfeather_dsd.so` together with FeatherW8's JNI code. The
complete corresponding source for that library — both the LGPL code and
FeatherW8's own JNI code — is in this repository. You are free to modify the LGPL
components and rebuild the APK yourself by following [BUILD_LOCAL.md](BUILD_LOCAL.md);
the build uses nothing that is not in this repository and the public Android SDK/NDK.

This notice is a good-faith summary, not legal advice.

## Android and Kotlin libraries — Apache-2.0

Pulled in through Gradle at build time and not modified:

- AndroidX (Activity, Compose, Material 3, Material Icons Extended)
- AndroidX Media3 (ExoPlayer, Extractor, Session)
- Kotlin and kotlinx.coroutines
- JUnit 4 (test only, EPL-1.0)

Their licenses are published with the respective artifacts.
