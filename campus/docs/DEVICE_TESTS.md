# Galaxy S23 acceptance checklist — NOT RUN in the build environment

Record date, exact phone model, Android/One UI version, app version, selected TTS engine/voice, and recognition language. All boxes start unchecked. Only mark results after observing them.

## Permissions and camera

- [ ] Fresh install; deny camera. Clear error, no crash; Start and App settings remain available.
- [ ] Grant camera in Settings; return and Start. Rear-camera preview appears.
- [ ] Stop. Camera privacy indicator disappears and labels/boxes clear; late results do not speak.
- [ ] Rapid Start/Stop/Start ten times. No crash, duplicate camera binding, or callbacks from old session.
- [ ] Open another camera app, revoke permission, and retry. Failure is announced or displayed; recovery is possible.
- [ ] Lock/unlock; Home/return; rotate portrait/landscape. Assistance stops on lifecycle exit and resumes only with Start.

## Real inference

- [ ] Show a chair, bottle, person, and several model-supported objects. Record correct/incorrect/missed results without changing labels to match expectations.
- [ ] Move an object to all four preview corners, portrait and landscape. Boxes align after rotation/letterboxing.
- [ ] Adjust threshold. Low scores are filtered; it does not change the model's class vocabulary.
- [ ] Remove all objects. Old boxes clear within one second. No claim that the path is safe or clear.
- [ ] Read large printed English text, then small text, glare, blank paper, and blur. Empty/low-confidence outputs are handled without invented text. Check OCR output against actual text; OCR can still make errors.
- [ ] Press Read repeatedly, Stop while OCR runs, and restart. No old OCR result appears in the new session.

## Speech and commands

- [ ] Hear Start, object summaries, Read text, Stop, and failure feedback. Verify media volume and selected output.
- [ ] Repeat twice. It repeats the same previous content without recursively adding prefixes.
- [ ] Stop speaking immediately stops audio; duplicate object descriptions do not flood the user.
- [ ] Deny microphone. Touch controls still work.
- [ ] On-device commands: Start/Stop/Read/Repeat. Document unsupported English model, unavailable recognizer, and unknown command behavior.
- [ ] Command processing does not hear the app's own TTS; Cancel voice command restores spoken output.
- [ ] Remove/download TTS voice through system settings as practical. App states missing voice instead of claiming speech works.

## Offline verification — test each independently

Disconnect USB and turn on airplane mode; also confirm Wi-Fi and mobile data are OFF. Restart the app and phone if practical so a previous session cannot conceal a dependency.

| Capability | Observed result / failure | Pass? |
|---|---|---|
| Object detection after fresh launch | Not tested | — |
| Bundled Latin OCR | Not tested | — |
| English TTS | Not tested | — |
| Voice commands with installed language model | Not tested | — |
| Start/Stop/Read/Repeat touch controls | Not tested | — |
| No laptop dependency | Not tested | — |

No TTS or recognition language download is attempted by the app. Installing missing system language data beforehand may require internet. Builds need internet until dependencies are cached.

## Reliability/performance

- [ ] Run 15 minutes with phone unplugged, then repeat OCR and speech periodically.
- [ ] Copy performance reports at 1, 5, 10, and 15 minutes. Record battery percentage, device temperature/thermal state, processed FPS, inference p95, and arrival-to-decision p95.
- [ ] Provisional goals: 5 processed FPS and 500ms p95 capture-to-decision. The current receipt-based metric cannot prove the capture-to-decision goal. First establish sensor clock comparability or use external timing before judging that target.
- [ ] CameraX internal discarded frames are not counted. App throttle/stale counters are partial, explicitly labeled.
- [ ] Perform TalkBack navigation and 200% font-size tests. Every essential control must be reachable; critical text must not clip.
- [ ] For testing a missing model, remove/rename the asset in a temporary source copy: `:app:verifyModel` must fail. Restore the original asset before building. Runtime code also validates the model signature and catches initialization failure.

## Instrumented tests (physical S23 or compatible emulator)

`./gradlew :app:connectedDebugAndroidTest`

The instrumentation suite loads the real detector and runs it on a blank fixture, and tests OCR on generated printed text. Those fixtures are tests, never displayed as live detections. These tests do NOT validate real camera recognition, Bluetooth, microphone, speakers, or navigation. An emulator result is not a physical-phone result.
