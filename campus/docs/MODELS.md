# Models and dependency evidence

## Object detector

- Model: EfficientDet-Lite0 INT8, revision 1, input 320 x 320.
- Bundled file: `app/src/main/assets/models/efficientdet_lite0_int8.tflite`.
- Bytes: 4,602,795.
- SHA-256: `0720bf247bd76e6594ea28fa9c6f7c5242be774818997dbbeffc4da460c723bb`.
- Official artifact: https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/efficientdet_lite0.tflite
- Official model/task guide: https://ai.google.dev/edge/mediapipe/solutions/vision/object_detector
- Android API guide: https://ai.google.dev/edge/mediapipe/solutions/vision/object_detector/android
- Pinned runtime: `com.google.mediapipe:tasks-vision:0.10.32`.
- Runtime artifact verified against Google's Maven POM: https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-vision/0.10.32/tasks-vision-0.10.32.pom

The downloaded FlatBuffer signature and embedded labels were inspected. `labels.txt` is extracted from that exact model; placeholder `???` classes are not spoken. Examples of real labels: person, bicycle, car, chair, couch, bottle, book, dining table, toilet. There is **no door class**, no distance, no navigable-path segmentation, and no collision predictor.

EfficientDet-Lite0 was selected as the documented mobile baseline. SSD MobileNetV2 INT8 is documented as lighter/faster with an accuracy trade-off, but has **not** been benchmarked against it on the S23. The choice is provisional until physical measurements. No model training or custom classes are claimed. Model verification is a build task and runtime header validation; neither proves detection accuracy.

The Gradle `verifyModel` task checks the bundled asset's size, TFL3 signature, and SHA-256 against `model_manifest.json`. The model is already present in this source package. To restore it, download the exact official URL above to the same asset path, then compare its checksum with the manifest. It is never downloaded by the installed app.

## OCR

`com.google.mlkit:text-recognition:16.0.1` is the bundled Latin-script package. It embeds the recognition model at build time; the separate Play-services download variant is not used.

Guide and exact dependency: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
Supported scripts/languages: https://developers.google.com/ml-kit/vision/text-recognition/v2/languages

Phase 2 UI/commands/output are English. OCR recognizes Latin script (including English); other Latin-language accuracy is not validated here. There is no Kannada, Devanagari, Chinese, Japanese, or Korean model in this app. Known low line-confidence results below 0.5 are omitted. A missing confidence value is not converted into a fabricated confidence score.

## Build dependencies

- AGP 8.9 family compatibility: https://developer.android.com/build/releases/agp-8-9-0-release-notes
- CameraX 1.4.2: https://developer.android.com/jetpack/androidx/releases/camera
- Kotlin Android compatibility: https://developer.android.com/build/kotlin-support
- Gradle distribution: https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
- On-device recognition availability: https://developer.android.com/reference/android/speech/SpeechRecognizer
- TTS voice network flag: https://developer.android.com/reference/android/speech/tts/Voice
- Camera image analysis/backpressure: https://developer.android.com/media/camera/camerax/analyze
- Preview transformation: https://developer.android.com/reference/androidx/camera/view/transform/CoordinateTransform

Runtime APIs and byte ownership were also checked against official CameraX source JAR and MediaPipe source. MPImage closes/recycles its bitmap, so detection receives a separate owned copy rather than recycling the frame that OCR may still need.

## Terms and redistribution

Third-party components and model assets remain subject to their upstream licenses/terms. MediaPipe Tasks POM identifies Apache License 2.0 for the runtime; AndroidX source includes Apache-2.0 notices. ML Kit is distributed under Google's SDK/API terms. Model download location and hash are retained for provenance; this project does not claim ownership of those weights. Consult the upstream model documentation and terms before publishing a redistributed product.
