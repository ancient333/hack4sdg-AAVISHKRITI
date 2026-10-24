# Signal — standalone communication prototype

React + Vite + TypeScript, a Node/WebSocket relay, a procedural Three.js avatar, and MediaPipe Hand Landmarker. No Docker or paid API keys. Use Node 24 (verified), or Node 22.18+ for the TypeScript-based tests.

## Install and run

In PowerShell, from this folder:

```powershell
npm.cmd ci
npm.cmd run dev
```

Open http://localhost:5173. Stop an existing development process with Ctrl+C before restarting: backend code does not hot-reload. Vite uses strict port 5173 and proxies /ws and /api to port 3001. Use npm instead of npm.cmd on macOS/Linux.

Production:

```powershell
npm.cmd run build
npm.cmd start
```

Open http://localhost:3001. For an alternate production port, set $env:PORT='3004' before starting. Do not run production and development backends on the same port.

```powershell
npm.cmd test
```

Predev/prebuild synchronize the locally installed MediaPipe WASM runtime. The downloaded 7.8 MB hand model is included in public/models; provenance and checksum are in THIRD_PARTY.md. Model/runtime requests stay on the app's origin, with no model CDN needed during use. Google Fonts are optional and fall back to system fonts.

## Sign to Speech: what is real

Start Camera requests the browser's permission and displays its stream. The hand model loads when entering this mode. Once video is playing, the application processes new frames at up to 10 Hz using MediaPipe's VIDEO detector, draws the actual 21-point skeleton for each detected hand, and reports frame and hand counts. The video and overlay share the same contain sizing and mirror transform. Stopping, cancelling, or leaving the mode cleans up video tracks/inference callbacks; leaving also releases the model.

Inference currently runs synchronously on the main thread at a throttled rate; slower devices may benefit from a future worker implementation. Camera denial, missing devices, model loading failures, and inference failures have explicit feedback and retry paths.

**Hand tracking is not sign recognition.** No trained sign-language classifier was found or added. The output therefore remains empty rather than inventing words. The typed SignClassifier interface and null implementation in src/services/handTracking.ts mark this boundary. A genuine classifier can produce text through Camera's onRecognized callback into the read-only output, existing Play Audio action, and paired-session reply transport. This extension path is implemented but cannot be verified with real recognized words until a classifier is supplied. No 3D avatar appears in this mode.

## Text to Sign: limited vocabulary, arbitrary input

Any text up to 1000 characters can be submitted, including browser speech-transcribed text when that browser supports dictation. It immediately appears locally without a paired device. Full sentences stay visible.

src/services/gestures.ts builds a lexical plan using the available procedural demonstrations: Hello, Welcome, Yes, No, Thank you, Help. Phrases are matched before individual words; case and punctuation are normalized. Known entries are queued in source order; unknown words are listed without silently dropping or substituting them. For example:

- “Hello, thank you. Please help.” queues Hello → Thank you → Help and lists “Please” as unsupported.
- “How are you?” is accepted and shown in full, with How, are, you listed as unsupported; it does not trigger a random animation.

src/services/useAnimationQueue.ts plays these entries sequentially. Each clip lasts five seconds and returns to neutral; Reset pose clears the queue. Demo buttons replace the queue with the selected clip. Incoming messages enqueue just like local ones, while server echoes and delivery receipts do not replay duplicates. Switching to camera mode pauses the queue; returning resumes the current clip from its beginning.

The avatar has shoulders, elbows, wrists, palms, and finger groups, including a two-arm wave. **These are unverified demonstrations, not accurate ASL/ISL translation.** No trained translator or linguistically validated animation assets exist in this project. Adding arbitrary made-up poses for unsupported words would not fix that dependency.

## Pairing and delivery

1. Connect both devices to the same network and run the app on the laptop.
2. In Text to Sign, select the connection button and Create receiving session.
3. /api/network discovers non-loopback IPv4 addresses and supplies a LAN URL automatically. If multiple adapters exist, select the address reachable from the phone. A custom trusted HTTPS URL can also be entered. QR/link generation rejects localhost and loopback.
4. Scan the QR or open its URL, then select Join. The receiving device must approve. The session identifier is not an authentication credential.
5. Submit a sentence. Each local entry records its transport progress:
   - Processed locally, no paired device.
   - Processed locally, sending to server.
   - Sent to server (server echo received), waiting for receiving device.
   - Delivered to receiving device (receipt from that client; not a human read receipt).
   - Remote failed/disconnected/unconfirmed; local text remains.
6. Both endpoints can send text and animate supported entries. Sign-camera replies still require a genuine classifier.

LAN HTTP supports text messaging, but phone camera access generally requires trusted HTTPS. Windows Firewall, guest Wi-Fi isolation, VPN routes, and different networks can prevent reachability. The app does not alter firewall rules or bypass certificate warnings. LAN URL discovery is not proof that a particular phone can reach that address.

Sessions are approved, ephemeral two-client rooms, with bounded message size/rate and no disk message storage. History retains at most 100 messages, survives pairing changes, and clears on page reload. Old local messages are not sent retroactively. HTTP/WS is unencrypted; production public hosting needs HTTPS/WSS, stronger authentication, and abuse controls. Speech dictation may use the browser vendor's online service.

## Verification actually performed for Task 6

- Production TypeScript/Vite build passed. Only the non-fatal bundle-size warning remains.
- Four automated tests passed: phrase planning/unsupported words; network-address filtering; legacy exact matching; real WebSocket pairing, bidirectional transport, receipt ownership, validation, rejection, disconnect.
- Browser loaded the local hand model successfully.
- Real model inference on Google's recorded sample photo detected **2 hands, 21 landmarks each**, and rendered the skeleton. This is not a live-camera test.
- Live-camera permission was pending in the test browser. Physical live frames, hand detections from that camera, and hardware stop were **not verified**.
- Browser verified arbitrary sentence submission, complete-text retention, missing-word labels, sequential Hello → Thank you → Help playback, and receiving-side queueing over paired transport.
- The generated LAN address returned HTTP successfully and a second browser client joined through it. Messages received delivery receipts. No physical phone or QR scanner was tested.
- No real recognized text exists without a classifier. The prior speech-synthesis test reported completion for entered text; audible speaker output and classifier-to-speech end-to-end behavior remain unverified.

To reproduce the independent inference check, run the dev server and open http://localhost:5173/tests/vision.html. Click Run sample-frame inference. The page is development-only, uses the same tracker/overlay code, and explicitly identifies its input as a recorded sample. Test photo attribution: THIRD_PARTY.md.

## Files

- src/components/Camera.tsx and src/services/handTracking.ts: real hand inference and overlay.
- src/components/Avatar.tsx: procedural articulated avatar.
- src/services/gestures.ts and useAnimationQueue.ts: limited-vocabulary planning and ordered playback.
- src/services/session.ts and server/session.mjs: local-first history, messaging and delivery receipts.
- server/network.mjs and server/index.mjs: LAN discovery and static/API/WebSocket serving.
- src/App.tsx: integration; camera output, speech, pairing and message UI.
