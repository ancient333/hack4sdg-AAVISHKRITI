# Phase 3.1 — concise scene descriptions

## Design and scope

The baseline spoke a list of up to three distinct labels. It had no scene snapshot, object counts, or on-demand description command. This update adds deterministic English descriptions from actual detector records. It does not use a language model, cloud API, network connection, new detector, or additional camera/inference loop.

`SceneDescriptions` stores a copied list from one accepted frame, together with its session generation and camera-adapter receipt time. Every request rechecks availability, session, age (at most 1,000 ms), and the selected confidence threshold. A new observation replaces the previous one. Start/stop clears the snapshot. Empty results explicitly say the surroundings could not be identified clearly. Stale results say no recent camera observation is available.

Labels are grouped and sorted by count, then alphabetically, with at most three groups in the response. For example, two person detections and one chair detection produce “I can identify two people and a chair.” A bottle and dining table produce “I can identify a bottle and a dining table.” The system does **not** infer that the bottle is on the table. Counts describe detector records; inaccurate or duplicate model predictions can still produce inaccurate counts.

Automatic output remains limited to once per five seconds with the existing eight-second identical-message suppression. Empty results are explained on demand rather than announced continuously. The new accessible **Describe my surroundings** button and English voice commands **describe**, **describe my surroundings**, **describe surroundings**, and **what can you see** reuse the same snapshot. Repeated commands are debounced for 1.5 seconds. Explicit requests can repeat a fresh result after speech finishes; they cannot queue duplicates of the currently spoken sentence.

On-demand descriptions use RESULT priority; automatic descriptions use DESCRIPTION priority. SYSTEM speech always has higher priority. The observation's original timestamp travels into the speech queue, so a scene cannot become fresh merely by waiting. A scene waiting behind a longer system/OCR message can expire without being spoken; request it again after that message finishes.

Camera lifecycle, background behavior, hardware integration, models, inference libraries, OCR implementation, and TTS adapter are unchanged from the saved Phase 2 baseline. Unfinished Phase 2.1 recovery edits are **not** included. After an interruption, use Start assistance if required. No Phase 3.2/3.3 or navigation work is included.

## Files changed

| File | Purpose |
|---|---|
| core/src/main/kotlin/org/campuslens/core/SceneDescriptions.kt | New recent-observation component, grouping, wording and age-preserving speech events |
| core/src/main/kotlin/org/campuslens/core/Contracts.kt | DESCRIBE command, voice aliases and explicit-repeat flag on speech events |
| core/src/main/kotlin/org/campuslens/core/AssistanceEngine.kt | Speech policy honors explicit repetition but rejects duplicate active utterances; session engine unchanged |
| app/src/main/java/org/campuslens/MainActivity.kt | Accessible button, command handling, current-frame snapshot, automatic wording and snapshot clearing |
| app/src/main/java/org/campuslens/input/AndroidVoiceCommands.kt | Spoken-command help includes the new command |
| core/src/test/kotlin/org/campuslens/core/SceneDescriptionsTest.kt | Grouping, counts, freshness, unavailable input, priority, duplication and parser tests |
| app/build.gradle.kts | Version 0.3.1 / code 3; dependencies unchanged |
| README.md, docs/, PROJECT_TREE.txt | Current instructions, design, boundaries and validation |
| validation/, distribution/ | Build evidence and corresponding APKs; historical evidence isolated under phase2-baseline |

## Rebuild and install on Windows

1. Extract the archive and open the `campuslens` directory in Android Studio.
2. Use JDK 17, SDK Platform 35, Build-Tools 35.0.0 and Platform-Tools. Let Android Studio generate the machine-specific `local.properties`. Initial dependency downloads need internet; scene descriptions themselves do not.
3. In PowerShell at the project root, run:

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n org.campuslens/.MainActivity
```

To install the included APK without rebuilding, replace the install path with `'.\distribution\CampusLens-debug.apk'`. Enable USB debugging and accept the S23 authorization prompt first.

The included APK was verified to use a different debug signing certificate from the saved Phase 2 APK. If that earlier APK is installed, an in-place update with this packaged APK will fail. To preserve an in-place development install, rebuild this source on your original machine using its existing debug key. Debug signing keys are machine-specific. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, build on the same machine/key used for the installed app. Alternatively, uninstall the old app **only if you accept losing its local app data and permissions**, then install the new APK:

```powershell
& $adb uninstall org.campuslens
& $adb install .\distribution\CampusLens-debug.apk
```

Optional real-device model/OCR instrumentation tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

These existing instrumentation tests exercise the model and OCR; they do not establish audible scene-description behavior. The following manual checks are still necessary.

## Galaxy S23 checklist — results must be recorded on the phone

Record Android/One UI version, app version, TTS voice, recognition language and airplane-mode setting. Test in a stationary, controlled setting.

- [ ] Before starting, tap **Describe my surroundings**. Hear that scene assistance is unavailable; no old objects should be described.
- [ ] Start assistance; wait for fresh detections. Show a supported bottle/chair/person and request a description. Wording should reflect detector labels, not unrelated objects.
- [ ] Arrange two well-separated supported objects of the same class. When the detector shows two records, verify the spoken count. Record model misses separately from sentence-generation errors.
- [ ] Show a bottle and table. Hear object names only; no invented “on the table,” distance, direction, identity or safety claim.
- [ ] Point at a scene with no reliable detections, or raise the confidence threshold. On demand, hear that the surroundings could not be identified clearly. Never hear “clear path.”
- [ ] Keep the scene still for 20 seconds. Verify automatic output is not frame-by-frame and identical speech is suppressed within the cooldown.
- [ ] After a description finishes, request it again while objects remain visible. Receive a fresh description. Rapid repeated taps must not create overlapping speech or start extra camera sessions.
- [ ] Use Voice command, then say **describe my surroundings** and **what can you see**. Verify touch controls also remain usable with microphone permission denied.
- [ ] Read English text, then request a scene; verify OCR still works. An ordinary automatic scene must not interrupt OCR. A scene must not interrupt a higher-priority system error.
- [ ] Stop assistance, change the scene, then request a description. Hear unavailable assistance, not cached object names. Restart and verify new observations are used.
- [ ] If the stream stalls or the camera becomes unavailable, request a description: no observation older than one second should be announced as current. This timing path is also covered by unit tests; the phone check requires a reproducible stall.
- [ ] Lock/unlock or leave/return as a regression check only. Use **Start assistance** as needed—the accepted manual-restart limitation is unchanged. Do not count it as a new Phase 3.1 failure.
- [ ] Enable airplane mode and disconnect the laptop. Repeat scene requests, OCR, TTS and supported on-device commands.
- [ ] With TalkBack and enlarged text, locate and activate the new button. Recheck Start/Stop, Read text, Repeat and Stop speaking.

## Limits

This is a detection-based assistive prototype, not a certified mobility system. Detection can miss or misclassify objects. Counts are model predictions, the list is capped at three classes, and the current model's class set is unchanged. No doors/stairs recognition, identities, spatial relationships, distance estimates, movement instructions, hazard predictions or safe-crossing decisions are added. English wording and the existing offline language/voice availability requirements remain.

Freshness is measured from adapter receipt time, not independently verified sensor capture time. The one-second limit may reject results under load; it favors an explicit lack of recent information over stale descriptions. Physical S23 scene quality, speech timing and accessibility must be retested. See VALIDATION.md for actual automated results.
