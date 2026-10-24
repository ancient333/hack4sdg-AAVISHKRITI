# Prototype object-distance estimates

This patch adds an approximate distance label for selected detected object categories.

## How it works
- Uses the detected bounding-box width in pixels.
- Uses a nominal 60-degree horizontal camera field of view.
- Assumes a typical real-world width for each supported label.
- Displays and periodically speaks the nearest estimate, with an explicit approximation warning.

## Important limitations
This is **not measured depth**. Actual distance can be substantially wrong when the object is smaller/larger than the assumed size, partly occluded, rotated, detected with a loose bounding box, or viewed using a camera with a different field of view. Estimates are capped at 8 metres and unsupported labels show "distance unavailable". It must not be used for collision avoidance, stairs, road crossings, or other safety-critical navigation.

For reliable metric distance, the next step should be a depth-capable sensor/API (for example, a validated ARCore Depth API pipeline on supported devices) or a calibrated stereo/depth camera. The current CameraX pipeline owns the camera session, so introducing ARCore Depth requires deliberate camera/session integration rather than simply adding a model file.

## Verification status
The source patch was inspected, but an Android build could not be run in this environment because Gradle 8.11.1 could not be downloaded (`UnknownHostException: services.gradle.org`). Build and Galaxy S23 runtime validation remain required.
