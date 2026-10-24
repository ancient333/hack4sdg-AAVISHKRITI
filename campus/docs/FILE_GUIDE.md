# File guide

| Files | Purpose |
|---|---|
| Root Gradle files, wrapper scripts and JAR | Reproducible Android Studio/CLI build entry points |
| app/build.gradle.kts | Android SDK/dependency versions and model-integrity gate |
| core/build.gradle.kts | Pure Kotlin module and JUnit dependency |
| AndroidManifest.xml | Camera/microphone permissions, speech service visibility, launcher; no Internet permission |
| core/Contracts.kt | Frame, detection, OCR, commands, and speech interfaces |
| core/SceneDescriptions.kt | Recent immutable detector snapshot, grouping, concise wording, stale/unavailable responses |
| core/AssistanceEngine.kt | Session tokens, freshness, confidence/OCR policies, bounded prioritized speech |
| MainActivity.kt | Accessible controls, permission flow, lifecycle, worker orchestration, metrics |
| input/PhoneCameraSource.kt | CameraX preview/analysis, crop/rotation mapping, frame ownership and cleanup |
| input/AndroidVoiceCommands.kt | On-device recognizer, five commands, cancellation and timeout |
| inference/LocalVision.kt | Bundled MediaPipe detector and separate ML Kit OCR reader |
| output/AndroidSpeechOutput.kt | Offline voice selection, speech queue, interruption, repeat, cleanup |
| ui/DetectionOverlay.kt | Camera-aligned graphical labels and confidence values |
| res/ | App name, theme, vector icon, backup exclusions |
| assets/models/ | Detector, extracted labels, source/hash manifest |
| core/src/test/ | Original policy tests plus focused scene-description tests; see VALIDATION.md for executed results |
| app/src/androidTest/ | Real-runtime model/OCR smoke tests; compiled but not device-executed |
| docs/ | Architecture, model sources, boundaries, phone checklist, validation |
| validation/ | Actual build, test, lint, failure-case, and APK-check evidence |
| distribution/ | Built debug app and instrumentation APK |
