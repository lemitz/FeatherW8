# Developing FeatherW8

How to build, test and release the app. For what the code does see [architecture.md](architecture.md); for the
detailed local setup see [BUILD_LOCAL.md](../BUILD_LOCAL.md). A [Russian version](development.ru.md) is
available.

**Contents**
[Requirements](#requirements) ·
[Building](#building) ·
[Testing](#testing) ·
[Continuous integration](#continuous-integration) ·
[Signing](#signing) ·
[Versioning](#versioning) ·
[Releasing](#releasing) ·
[Working on a change](#working-on-a-change) ·
[Pinned versions](#pinned-versions)

---

## Requirements

- JDK 21
- Android SDK Platform 37, Build Tools 36.0.0
- Android NDK `27.2.12479018` and CMake `3.22.1` (for the native DSD converter)
- An `arm64-v8a` device or emulator to run it; the app ships only the 64-bit ARM native library

`BUILD_LOCAL.md` has the `sdkmanager` commands and a low-memory build script.

---

## Building

From the repository root:

```sh
./gradlew :app:assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

(`gradlew.bat` on Windows.)

The **debug** build is unoptimised and noticeably slower; use the **release** build when judging performance. It
runs R8 (code and resource shrinking, rules in `app/proguard-rules.pro`), and is what is published. The release
build is signed with the standard Android debug key unless you configure your own (see [Signing](#signing)), so
it installs on your device but cannot be installed over an official release.

Lint is switched off for release builds (`lint { checkReleaseBuilds = false }`) so that it cannot fail them.

---

## Testing

```sh
./gradlew :app:testDebugUnitTest
```

JVM unit tests only; see [architecture.md](architecture.md#testing) for what they cover. There are no
on-device tests: UI behaviour, the playback service and the JNI converter are checked by hand on a phone.

Release builds are worth installing and trying before a release, because R8 can break what works in debug
(for instance the JNI class `NativeDsdDecoder`, which is kept explicitly in `proguard-rules.pro`).

---

## Continuous integration

`.github/workflows/build.yml` runs on pushes to `main`, pull requests, `v*` tags and manual dispatch. A newer run
on the same ref cancels the older one.

**`build` job** (Ubuntu 24.04, JDK 21, Android SDK 37 / build-tools 36.0.0 / NDK / CMake as above):

1. Restores the signing key from the repository secrets, if they exist.
2. Runs `./gradlew :app:testDebugUnitTest :app:assembleRelease`.
3. Names the APK `FeatherW8-v<version>-arm64-v8a.apk`, reading the version from `appVersionName`.
4. Prints the signing certificate's SHA-256, so it can be compared between builds.
5. On a tag, checks that the tag equals `v<appVersionName>`.
6. Uploads the APK as an artifact (and the test reports if something failed).

**`release` job**, only for `v*` tags and only after `build` succeeded: creates the GitHub Release with the APK
and, as the notes, the contents of `docs/release-notes/<tag>.md` (or generated notes if that file is missing).

Pull requests from forks do not receive secrets; their builds fall back to the debug key and still succeed.

To get the APK of any run onto your machine:

```sh
gh run list --branch <branch> --limit 1
gh run download <run-id> --dir apk
```

---

## Signing

Official releases are signed with a project key. It is **not in the repository**.

| Where | What |
|---|---|
| Repository secret `FEATHERW8_KEYSTORE_BASE64` | The keystore file, base64-encoded |
| Repository secret `FEATHERW8_KEYSTORE_PASSWORD` | Its password (the key password is the same, as in a PKCS12 keystore) |
| Key alias | `featherw8` (set in the workflow) |

The keystore is a PKCS12 file with an RSA 4096 key valid for 100 years. Its certificate SHA-256 fingerprint is
`5d82000615449058440720287495cee52bcabdb058c1d6051518154ab1ea13ac` (the build log prints it each time).

**Keep an off-machine backup of the keystore and its password.** Android installs an update only over an app
signed with the same key. If the key is lost, nobody can update in place: users would have to uninstall (and
lose their data), and the app would be signed with a new key from then on. There is no way to rotate the key
without that break.

Gradle reads the key from environment variables, so the same mechanism works locally:

```text
FEATHERW8_KEYSTORE=<path to the .jks>
FEATHERW8_KEYSTORE_PASSWORD=<password>
FEATHERW8_KEY_ALIAS=<alias>
```

Without `FEATHERW8_KEYSTORE`, builds use the standard debug key.

To create a new key (a fresh project, or a fork that wants its own):

```sh
keytool -genkeypair -keystore featherw8.jks -storetype PKCS12 -alias featherw8 \
        -keyalg RSA -keysize 4096 -validity 36500 -storepass <password> -keypass <password> \
        -dname "CN=FeatherW8, O=<you>"
base64 -w0 featherw8.jks | gh secret set FEATHERW8_KEYSTORE_BASE64
gh secret set FEATHERW8_KEYSTORE_PASSWORD
```

---

## Versioning

Versions are `MAJOR.MINOR.PATCH`, chosen by how much a change matters to someone who updates:

- **PATCH** (1.4.0 → 1.4.1): bug fixes only; nothing to relearn.
- **MINOR** (1.4.0 → 1.5.0): new features or visible behaviour changes that keep the user's data working.
- **MAJOR** (1.x → 2.0.0): a change that can break existing data or removes a feature.

The version is written **once**, in `app/build.gradle.kts`:

```kotlin
val appVersionName = "1.4.1"
```

and `versionCode` is derived from it (`1.4.1` → `10401`, that is `major × 10000 + minor × 100 + patch`), so the two
cannot disagree and the code always grows with the version. The README's version badge is updated by hand with
each release.

---

## Releasing

1. Make the change on a branch and open a pull request. CI must be green.
2. In the same pull request:
   - raise `appVersionName` as described above;
   - add `docs/release-notes/v<version>.md` (a few bullet points, then an "APK notes" line);
   - add the same text to [`CHANGELOG.md`](../CHANGELOG.md);
   - update the README badges (`README.md`, `README.ru.md`) and anything else the change touches.
3. Install the APK from the pull request's CI run on a phone and try it.
4. Merge. The history uses ordinary merge commits.
5. Tag the merge commit and push the tag:

   ```sh
   git switch main && git pull --ff-only
   git tag -a v<version> -m "FeatherW8 <version>"
   git push origin v<version>
   ```

6. The tag run builds, signs and publishes the release. Check that the release has the APK, the right notes, and
   that the log says "Signing with the project key" with the usual fingerprint.

The tag must equal `v` plus `appVersionName`, or the build stops.

---

## Working on a change

The commit history follows the usual prefixes: `feat`, `fix`, `perf`, `refactor`, `docs`, `test`, `ci`, `build`,
`style`, `chore`. A few habits that have paid off:

- **Say why** in the commit message, not just what; several fixes here came from a non-obvious cause.
- **Prefer pure functions** for anything with edge cases and test them on the JVM (`ListEdits`,
  `ActualPlayCounter`), since there are no on-device tests.
- **Keep Compose state at the top and screens stateless**; new screens and dialogs take values and callbacks.
- **Verify on a device**, release build, for anything that touches playback, gestures, the notification or R8.

---

## Pinned versions

| | |
|---|---|
| Gradle | 9.4.1 |
| Android Gradle Plugin | 9.2.0 |
| Kotlin / Compose compiler plugin | 2.3.10 |
| Compose BOM | 2026.09.00 |
| AndroidX Media3 | 1.11.1 |
| kotlinx.coroutines | 1.10.2 |
| Compile / target / min SDK | 37 / 36 / 28 |
| NDK / CMake | 27.2.12479018 / 3.22.1 |
| Java target | 17 |
