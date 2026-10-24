# Campus Lens — Phase 3.1 scene descriptions (0.3.1)

Native Kotlin application for a Samsung Galaxy S23. The phone runs camera capture, bundled object detection, bundled Latin-script OCR, assistance policies, and speech output. No backend, laptop camera, laptop server, or mandatory cloud AI service exists.

Phase 3.1 adds short, local scene descriptions from recent detector results. Camera interruption behavior is unchanged: after lock/unlock or leaving the app, press **Start assistance** if needed. See `docs/PHASE_3_1.md` for changes and the focused phone checklist.

## Included built APK

`distribution/CampusLens-debug.apk` is the compiled development app. The matching instrumentation APK is also included. Open `docs/VALIDATION.md` for actual results and outstanding phone tests.

## Open and run on Windows

1. Install Android Studio (VS Code and Python alone are insufficient for an Android build). Open this **campuslens** directory, the one containing `settings.gradle.kts`.
2. In SDK Manager install **Android SDK Platform 35**, **Build-Tools 35.0.0**, and **Android SDK Platform-Tools**. Accept their licenses. Use **JDK 17** as the Gradle JDK under Settings > Build, Execution, Deployment > Build Tools > Gradle. Android Studio may offer a JDK download.
3. Allow Gradle sync. First build requires internet to obtain build dependencies; the detector is already included. Do not change plugin versions just because a newer one is offered.
4. On the S23: Settings > About phone > Software information > tap Build number seven times; enable USB debugging in Developer options. Connect by USB and accept the phone's debugging prompt. Windows may need Samsung's USB driver.
5. Select the S23 as the run device and **app** as the run configuration. Click Run. If no device appears, use `adb devices` and check the cable and authorization.
6. Open Campus Lens, press **Start assistance**, and grant Camera permission. Point the rear camera at a chair, bottle, or other supported object. Use **Read text** with a large, well-lit English sign. Repeat replays the previous spoken message, explicitly identified as previous.
7. Optional voice commands: tap **Voice command**, grant Microphone permission, then say “start assistance”, “stop assistance”, “read text”, “describe my surroundings”, or “repeat”. Recognition is one command per tap, not always listening. Only Android's on-device recognizer is used; if it or the English language model is unavailable, use touch controls.
8. Tap **Describe my surroundings**, or request it through Voice command. Descriptions use only a current detector snapshot and name at most three object classes; no positions, distances, or relationships are inferred.
9. If speech is unavailable, open **Text-to-speech settings**, install an offline English voice, and reopen Campus Lens. No voice is downloaded silently by this app.
10. Disconnect the laptop. Follow the airplane-mode and lifecycle tests in `docs/DEVICE_TESTS.md`. Initial model bundling is verified at build time; actual offline behavior is not established until these tests run on the phone.

## Build commands (PowerShell, project root)

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

`installDebug` and `connectedDebugAndroidTest` require a connected Android device. You may invoke `adb` using its full path, normally `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`.

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

On Linux/macOS substitute `./gradlew`. Build output: `app/build/outputs/apk/debug/app-debug.apk`. The debug APK is for development, not a Play Store release. First-run Gradle distribution downloads can take several minutes. `local.properties` is machine-specific; Android Studio creates it automatically. Never copy another machine's SDK path.

## Versions

| Component | Pinned version |
|---|---|
| Gradle | 8.11.1 |
| Android Gradle Plugin | 8.9.2 |
| Kotlin | 2.1.20 |
| Java toolchain | 17 |
| compileSdk / targetSdk | 35 / 35 |
| minSdk | 31 (Android 12+) |
| AndroidX Activity | 1.9.3 |
| AndroidX Core | 1.15.0 |
| CameraX core/camera2/lifecycle/view | 1.4.2 |
| MediaPipe Tasks Vision | 0.10.32 |
| Bundled ML Kit Latin text recognition | 16.0.1 |
| JUnit | 4.13.2 |
| AndroidX test runner / ext JUnit | 1.6.2 / 1.2.1 |

These are deliberately pinned compatible versions, not a claim to use the newest versions. Runtime native-library compatibility must be checked on the installed S23 OS. See `docs/VALIDATION.md` for what was actually executed.

## Architecture

- `core`: pure Kotlin contracts, session generations, confidence filtering, recent scene snapshots and label grouping, OCR text policy, priority queue and duplicate suppression. No Android camera, network, or Bluetooth code.
- `app/input`: CameraX rear-camera source and replaceable on-device command adapter.
- `app/inference`: local detector and independently implemented OCR reader.
- `app/output`: Android speech adapter with an offline-voice capability check, interruption, bounded waiting queue, and repeat.
- Automatic spoken scene descriptions and approximate nearest-object distance announcements are rate-limited to once every 15 seconds; identical speech messages are suppressed for 20 seconds to reduce repetitive alerts. Use on-demand controls/voice commands when you need an immediate update.
- `app/ui`: preview bounding-box overlay; controls/status are built by `MainActivity`.
- `MainActivity`: lifecycle, permissions, worker ownership, UI coordination, on-demand OCR, metrics, and failure recovery.

All inference runs on one worker. CameraX keeps only the latest pending frame. The adapter throttles processing to at most 8 frames/second; achieved throughput depends on the device. Each ImageProxy is closed in `finally`. Bitmap ownership is explicit. OCR uses a separate copy so a timed-out asynchronous OCR task cannot reference recycled camera pixels. Stop invalidates a session generation; late results from previous sessions cannot update UI or speak.

## Included / excluded

Implemented: rear camera preview, object labels/scores/boxes, configurable threshold, on-demand OCR, on-demand scene descriptions, speech/repeat, on-device recognizer availability check, touch controls, explicit camera and model errors, stop/retry, debug timing.

Not implemented: routing, destination search, distance estimation, hazard prediction, crossing decisions, Bluetooth camera streaming, Wi-Fi streaming, cloud scene descriptions, always-on background camera, Kannada OCR. No fabricated camera results.

See `docs/ARCHITECTURE.md`, `docs/MODELS.md`, `docs/DEVICE_TESTS.md`, `docs/VALIDATION.md`, and `PROJECT_TREE.txt`.


## Phase 4 update (source-only)
The current Phase 4 source adds voice-triggered current phone location awareness using “where am I” and related phrases. It does not provide turn-by-turn navigation or safety guidance. See `docs/PHASE_4.md`. The Phase 4 APK has not yet been built or device-tested; build it in Android Studio before installation.
