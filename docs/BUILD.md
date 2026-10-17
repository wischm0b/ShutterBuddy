# Building Shutter Buddy

## Requirements

- JDK 17 or later, for example Android Studio’s bundled runtime.
- Android SDK matching `compileSdk` in `app/build.gradle`: API 36, minor version 1. Set `ANDROID_HOME` or use an untracked `local.properties` file containing `sdk.dir`.
- PowerShell 7 for the Windows build scripts.
- Garmin Connect IQ SDK and the `fenix847mm` device definition, installed through Garmin SDK Manager. This project has been built with SDK 9.2.0.

```powershell
git clone https://github.com/wischm0b/ShutterBuddy.git
cd ShutterBuddy
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
```

## Android

The included Gradle 8.13 wrapper downloads its distribution on first use and verifies its pinned SHA-256 checksum.

```powershell
./gradlew.bat testDebugUnitTest assembleRelease lintDebug
# Also copies the APK into dist:
./build.ps1 -Target Android
```

Use `./gradlew` on Linux/macOS. Output: `app/build/outputs/apk/release/app-release.apk`; the PowerShell script also creates `dist/ShutterBuddy-<Android-version>.apk`.

Local release builds currently use the local Android debug signing key. Private keys are excluded from Git. A build made on another machine will usually have a different signature and cannot directly update an installed published APK. Use the GitHub release APKs to update that installation.

## Garmin

```powershell
./build.ps1 -Target Garmin -GarminSdk 'C:/path/to/ConnectIQ-SDK'
```

Alternatively set `GARMIN_SDK_HOME`. On Windows, the script also detects the SDK selected in SDK Manager. In the original workspace it reuses existing SDK/Gradle installations under `../.tools`; a fresh clone does not require those directories.

A Garmin signing key is generated locally at `garmin/developer_key.der` if needed and remains untracked. Output: `dist/ShutterBuddy-<Garmin-version>-fenix847mm.prg` plus an unversioned copy. Android and Garmin versions may differ; each release identifies the matching pair.

## Settings and languages

The phone owns watch feedback and language preferences. Each preference update increments a revision. Session-bound replies carry the latest snapshot, and changes are pushed to an active watch session. The watch rejects older revisions within a session and retains preferences across restarts. A new session accepts the phone’s current revision, including after a phone reinstall.

Android strings use English defaults and German resources. The phone’s explicit language choice also selects the watch’s messages. Service results are stored as resource IDs and arguments so changing language updates existing feedback.

Watch countdown feedback uses the existing timer: one gentle pulse per newly displayed positive second, without replaying skipped seconds. Shutter vibration/tone follows successful Pixel acknowledgement, including interval shots; it does not confirm that Pixel Camera saved a photo. No extra watch timer is allocated.

## Tests and artwork

Run Android unit tests through Gradle. Garmin tests use `(:test)` and require a working Connect IQ simulator to execute. In the original environment these tests could only be compiled; physical watch feedback still needs device testing.

Regenerate logo sizes from the selected master without changing its design:

```powershell
./tools/Export-BrandAssets.ps1
```

Private signing keys, SDK installations, local paths, caches and build outputs are excluded by `.gitignore`. Installable files are distributed separately through GitHub releases.
