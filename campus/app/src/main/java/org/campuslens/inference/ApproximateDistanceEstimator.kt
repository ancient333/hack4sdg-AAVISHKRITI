package org.campuslens.inference

import org.campuslens.core.Detection
import kotlin.math.roundToInt

/**
 * Prototype monocular estimate from apparent object width.
 * This is not a depth sensor and must not be used as safety-critical guidance.
 */
object ApproximateDistanceEstimator {
    // Approximate real-world widths in metres for common COCO/MediaPipe labels.
    private val widthMetres = mapOf(
        "person" to 0.45f,
        "bicycle" to 0.60f,
        "car" to 1.80f,
        "motorcycle" to 0.80f,
        "bus" to 2.50f,
        "truck" to 2.40f,
        "chair" to 0.45f,
        "couch" to 1.80f,
        "bottle" to 0.075f,
        "cup" to 0.09f,
        "book" to 0.15f,
        "laptop" to 0.33f,
        "tv" to 1.00f,
        "dining table" to 1.20f,
        "backpack" to 0.30f,
        "handbag" to 0.25f,
        "suitcase" to 0.45f,
        "potted plant" to 0.35f,
        "bench" to 1.20f,
        "bed" to 1.50f
    )

    /**
     * Estimates range in metres using a nominal 60-degree horizontal field of view.
     * Since object size and camera field-of-view vary, results are explicitly approximate.
     */
    fun estimateMetres(detection: Detection, frameWidthPx: Int): Float? {
        if (frameWidthPx <= 0) return null
        val realWidth = widthMetres[detection.label.trim().lowercase()] ?: return null
        val pixelWidth = detection.box.right - detection.box.left
        if (!pixelWidth.isFinite() || pixelWidth < 2f) return null
        val focalPixels = frameWidthPx * 0.8660254f // width / (2 * tan(30 degrees))
        val metres = focalPixels * realWidth / pixelWidth
        if (!metres.isFinite() || metres <= 0f) return null
        return (metres.coerceIn(0.2f, 8.0f) * 10f).roundToInt() / 10f
    }

    fun spokenDistance(metres: Float): String =
        if (metres < 1f) "${(metres * 100).roundToInt()} centimetres" else "${metres} metres"
}
