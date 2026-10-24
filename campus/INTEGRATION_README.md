# CampusLens + Signal Deaf Mode integration

The CampusLens top-right menu now opens a native Android `DeafModeActivity` containing the Signal communication interface in an embedded WebView. Blind Mode remains the launch/default screen. Back returns to CampusLens.

## Build on Windows

1. Install Node.js 22.18+ (Node 24 recommended by Signal's own README) and Android Studio/JDK 17.
2. Open PowerShell in this extracted project folder.
3. Run `powershell -ExecutionPolicy Bypass -File .\integration\build-deaf-mode.ps1`.
   - This runs `npm ci` and `npm run build` in `signal-web`, then copies the compiled web assets into `app/src/main/assets/deaf`.
4. Open the Android project root in Android Studio, sync Gradle, and build/install the APK.

## What works / dependencies

- The Signal UI is bundled into the Android APK after the script completes; it does not need a hosted web page just to open.
- Camera access for MediaPipe hand landmark tracking is requested by Android WebView. The app needs camera permission.
- Signal's own README is authoritative for limitations: hand tracking shows 21-point landmarks, but **there is no trained sign-language classifier in the supplied archive**, so it cannot reliably turn arbitrary signs into words.
- The separate `recognition_api.py` requires `models/isl_classifier.joblib`, which is not included. The integration does not start a Python server inside Android. To use that optional classifier API, a separately hosted/reachable Python API and compatible trained model are required, and the web app must be configured to call it. A/B/C-only model data only supports those labels.
- Pairing, cross-device message delivery, and LAN discovery depend on Signal's Node/WebSocket server. The bundled UI opens locally, but server-based pairing will not work unless that server is separately deployed/reachable and configured. Offline local text entry/audio and local hand tracking do not require that relay.
- Text-to-speech uses the device/browser speech synthesis support. Speech dictation support varies by Android WebView/device.

## Security note

This is a prototype. Do not expose the relay publicly without HTTPS/WSS, authentication, and abuse controls. Hand tracking is not sign-language translation; do not present it as a reliable interpreter.
