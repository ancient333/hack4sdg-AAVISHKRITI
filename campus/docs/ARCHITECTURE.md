# Runtime design and limits

## Phone-only execution

The release and debug app both select the phone rear camera. There is no host URL, HTTP client, backend process, or laptop webcam adapter. INTERNET and ACCESS_NETWORK_STATE permissions are explicitly removed in the merged manifest. Android speech services are separate system services: the app selects a non-network TTS voice and explicitly creates an on-device SpeechRecognizer, but airplane-mode tests must establish behavior on the S23.

## Frame and result contracts

`FrameSource<T>` produces `FramePacket<T>` with source identifier, capture timestamp, adapter receipt timestamp, dimensions, and normalized orientation. The camera adapter converts RGBA ImageProxy to an upright bitmap. The consumer completes synchronously before the adapter releases pixels. A future hardware adapter can produce the same bitmap contract without changing detection, OCR, or the core policies.

`capturedNs` is a sensor timestamp with a source-specific clock domain. It is intentionally **not** subtracted from elapsedRealtime until clock comparability is established. Current timing is adapter-arrival-to-UI-decision, not capture-to-decision; the UI says so. Future adapters must specify the capture clock and transport delay.

The Android bitmap packet carries CameraX OutputTransform separately for visualization. The camera buffer is cropped to the shared ViewPort and rotated once; detections in that upright cropped image are, then mapped through ImageProxyTransformFactory (rotation and crop origin enabled) to PreviewView's OutputTransform. Both use cases share a ViewPort. FIT_CENTER letterboxing is accounted for. Only the rear camera is used, so front-camera mirroring is out of scope. Physical tests must check portrait, landscape, edges, and aspect ratios.

The engine rejects observations older than one second and any results with a previous session token. Overlay and labels clear when fresh results stop. Explicit OCR requests may finish later and are labeled as a text result; they never become obstacle guidance.

## Speech / command behavior

- Ordinary scene descriptions: at most once per five seconds, at most three label groups ordered by count then label, eight-second identical-message suppression. Counts come from one detector frame, never an accumulation across frames.
- Describe my surroundings: accessible button or push-to-talk command. Reuses a copied detection list, checks current session, confidence threshold and one-second receipt age. No additional inference job is scheduled. Requests are debounced for 1.5 seconds; a deliberate request can repeat a fresh result after the previous speech finishes.
- Empty/low-confidence observations yield an explicit inability-to-identify response on demand. Missing/stale observations and unavailable assistance have distinct responses. Positions and relationships are not inferred.
- Priority: SYSTEM > RESULT > DESCRIPTION. A higher priority interrupts current audio. At most one waiting event per priority is retained.
- Scene descriptions expire one second after camera-frame receipt, including time already spent in inference and the speech queue. Ordinary scenes use DESCRIPTION priority; on-demand scenes use RESULT priority. Neither overrides SYSTEM speech. OCR results have a longer TTL.
- Repeat identifies content as a previous result, not a current observation.
- Push-to-talk cancels current speech and pauses announcements while recognition runs. Critical camera failure cancels recognition before announcing the failure.
- English commands only. No automatic fallback to a cloud recognizer. Installed on-device language support may be absent even if the recognizer service exists.
- Offline English TTS capability is checked on launch. Installed voice selection is not proof of successful audible playback or offline behavior.

## Lifecycle and recovery

No foreground service is implemented. On leaving the foreground or locking the phone, the Activity stops assistance, unbinds camera, cancels commands, clears current results/speech, and queues model cleanup on the same worker. Returning requires an explicit Start. Configuration recreation also stops the session. This intentional policy avoids claiming unverified background-camera behavior.

Permission denial leaves controls available and offers App settings. Initialization timeout, camera stream stall, camera state errors, and inference errors stop the session. The user retries with Start. Stop/start generations prevent old callbacks from reviving the camera. OCR failure is nonfatal to detection.

## Future glasses

Only interfaces are prepared. No Bluetooth stream, invented protocol, or Wi-Fi fallback is included. Future hardware must expose a documented compressed-frame transport with measured bandwidth, capture timestamps, orientation, decoding, automatic reconnection, and audio coexistence. A compatible Bluetooth audio path is independent of its camera protocol. Phone inference and core policies remain reusable. Future source loss must invalidate detections rather than repeat the last frame as current.

## Accessibility and privacy

Large full-width controls (at least 56dp height), scalable sp text, high contrast, meaningful button labels, status live region, no color-only meaning, scrollable content, repeat and stop-speech controls. Camera preview and graphical boxes are excluded from screen-reader focus. Spoken observations are available independently of boxes. TalkBack interaction, high font scaling, headset routing, and outdoor noise must be physically tested. Camera images, OCR content, and microphone audio are not stored. Diagnostic logs contain timing/counters only, not image or text content.
