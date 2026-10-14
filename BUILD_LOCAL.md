# Build FeatherW8 locally

This bundle contains the complete Android Studio/Gradle project, Gradle Wrapper, app sources, bundled DSD-to-PCM native decoder, and its LGPL-2.1 license. No separate `dsd-nexus` checkout is needed. A machine-specific `local.properties` file is intentionally not included; Android Studio or the Android SDK environment variables should provide the SDK path.

## Prerequisites

- JDK 21 (select it as the Gradle JDK in Android Studio).
- Android SDK Platform `android-37.0`.
- Android SDK Build Tools `36.0.0` or newer.
- Android NDK `27.2.12479018`.
- CMake `3.22.1`.
- At least 8 GB RAM and several GB of free disk space are recommended for the first build.
- Internet access for the first Gradle sync/build; Gradle downloads the wrapper, Android Gradle Plugin, and Maven dependencies automatically.

The Android Studio SDK Manager can install the SDK packages above. From the command line, accept licenses and install them with:

```sh
sdkmanager --licenses
sdkmanager "platforms;android-37.0" "build-tools;36.0.0" "ndk;27.2.12479018" "cmake;3.22.1"
```

Set `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) to the SDK directory if it is not configured by Android Studio. Do not copy another machine's `local.properties`; its SDK path is machine-specific.

## Build and test

Open the repository root in Android Studio and let Gradle sync, or run the commands below from the repository root. On Linux/macOS, the included script runs the unit tests and assembles the debug APK with Gradle and CMake limited to one worker. It uses an in-process Kotlin compiler and a conservative Gradle JVM configuration (768 MB heap, 448 MB metaspace, one active processor) to reduce memory pressure:

```sh
./build-local.sh
```

Or run the tasks directly:

```sh
CMAKE_BUILD_PARALLEL_LEVEL=1 ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1
```

On Windows PowerShell, set `$env:CMAKE_BUILD_PARALLEL_LEVEL = "1"` and run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1`. The app's debug APK will be at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is automatically signed with the standard Android debug key. The app targets `arm64-v8a`; it does not need an external keystore for a local debug install.

## Versions pinned by the project

- Gradle Wrapper: `9.4.1`
- Android Gradle Plugin: `9.2.0`
- Kotlin Compose plugin: `2.3.10`
- Compile SDK: `37`
- Target SDK: `36`
- NDK: `27.2.12479018`
- CMake: `3.22.1`
