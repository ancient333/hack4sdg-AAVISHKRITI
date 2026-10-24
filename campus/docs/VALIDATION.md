# Phase 3.1 validation — 15 October 2026

Version 0.3.1 / code 3 was built from the saved Phase 2 project with focused scene-description changes. Unfinished interruption-recovery changes are not included.

## Executed in this environment

| Check | Result |
|---|---|
| Gradle build | PASS, exit 0; Gradle reported 6m 6s |
| Original Kotlin unit tests | 17 passed, zero failures/errors/skips |
| New scene-description unit tests | 16 passed, zero failures/errors/skips |
| Total unit tests | 33 passed |
| Debug APK | Assembled successfully, version 0.3.1 / code 3 |
| Android lint | 0 errors, 11 warnings: 10 SetTextI18n, 1 UseKtx |
| Existing model/OCR instrumentation APK | Compiled successfully; not executed |
| APK signature | Verified, v2 |
| APK ZIP alignment | Passed zipalign with 16 KB option |
| Packaged model SHA-256 | Matches the Phase 2 bundled model |
| App Internet permission | Absent from the APK permission dump |
| Camera source, detector/OCR implementation, TTS adapter, manifest | Byte-for-byte unchanged from saved Phase 2 |
| Lifecycle callbacks | Unchanged; accepted manual restart behavior retained |
| Emulator execution | NOT RUN; no emulator configured, /dev/kvm absent |
| Physical Galaxy S23 execution | NOT RUN; adb listed no attached devices |

Build tasks actually executed using downloaded Gradle 8.11.1, JDK 17, Android Platform 35 and Build-Tools 35.0.0:

```text
gradle --no-daemon :core:test :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest --console=plain
```

This environment additionally supplied its project path, proxy settings and Java trust store. Build duration includes first-run downloads and is not an inference-performance measurement. On Windows use the equivalent checked-in wrapper commands in PHASE_3_1.md.

The 16 new tests exercise actual Detection records, repeated-label grouping and counts, bounded stable ordering, empty/low-confidence results, threshold changes, snapshot replacement/copying, stale/future/session-invalid observations, unavailable assistance, automatic duplicate suppression, deliberate fresh repeats, higher-priority speech protection, queue expiry and command aliases. They are pure Kotlin tests: they do not simulate CameraX, Android TTS, a microphone or audible output.

## Toolchain issues and warnings

The old build toolchain was unavailable and was restored. The first SDK setup attempt failed because the extracted JDK lib/modules file was truncated. The downloaded archive's SHA-256 matched Adoptium metadata; the file was restored from that archive, javac ran successfully, SDK setup succeeded and the complete build then passed. No dependencies were changed to address this environment issue.

Gradle reported that debug symbols could not be stripped from four existing third-party native libraries; they were packaged intact. Lint warnings concern English strings in code and a Kotlin convenience suggestion. No lint baseline was added to hide errors.

The included APK's debug signing certificate differs from the saved Phase 2 APK. An in-place replacement of that earlier APK requires rebuilding with its original signing key; otherwise uninstall/reinstall is needed, resetting local app data and permissions. See PHASE_3_1.md for exact commands and alternatives.

## Physical-device evidence and remaining limits

The user reports prior S23 success for detection, OCR, offline speech/commands, Start/Stop, duplicate suppression and other Phase 2 checks. Those reports apply to the previous installed app. They are not new agent-performed tests and do not establish that the Phase 3.1 APK has passed phone testing.

Scene quality, audible descriptions, phone/headset audio routing, repeated requests under load, TalkBack interaction and airplane-mode behavior of this version remain for physical retesting. The accepted lock/unlock manual-restart limitation is unchanged. No distance, path-safety, collision, navigation or smart-glasses claims are made.

## Evidence files

Current: validation/build.log, build-status.json, test-summary.json, the two TEST-*.xml reports, lint-results.txt, apk-checks.json and phase3_1-source.patch.

Historical Phase 2 evidence is isolated under validation/phase2-baseline/. Its test/build claims apply only to that baseline. distribution/ contains the new debug app and matching compiled model/OCR instrumentation APK. The focused physical checklist and exact build/install commands are in docs/PHASE_3_1.md. Phase 3.2 and Phase 3.3 have not begun.
