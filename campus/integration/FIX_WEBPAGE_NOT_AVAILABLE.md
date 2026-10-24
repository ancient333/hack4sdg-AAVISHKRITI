# Fix: Deaf Mode webpage not available

The previous source ZIP did not include the compiled web interface at `app/src/main/assets/deaf/index.html`.

## Windows steps
1. Install Node.js 22.18+ or 24 from https://nodejs.org.
2. Extract this ZIP.
3. Open PowerShell in the `campus` folder.
4. Run: `powershell -ExecutionPolicy Bypass -File .\integration\build-deaf-assets.ps1`
5. Open the `campus` folder in Android Studio, sync Gradle, and build a new APK.

The script installs the Signal web dependencies, builds the web interface, and copies its `dist` output into Android assets. DeafModeActivity uses AndroidX WebViewAssetLoader to load local assets.

## Limitations
This bundles the interface only. A trained sign-language classifier is not included, so arbitrary signs will not yet be translated into words. Messaging/pairing features requiring the Node/WebSocket server need a reachable configured server; the laptop's `127.0.0.1` is not the phone. This ZIP has not been built on your machine.
