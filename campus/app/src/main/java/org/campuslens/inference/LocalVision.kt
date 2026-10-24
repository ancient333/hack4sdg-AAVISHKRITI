package org.campuslens.inference

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Tasks
import org.campuslens.core.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor

class LocalDetector(context: Context) : Detector<Bitmap> {
 private val detector: ObjectDetector
 init {
  context.assets.open(ASSET).use { ModelAssetValidator.requireTflite(it.readBytes()) }
  val base = BaseOptions.builder().setModelAssetPath(ASSET).build()
  detector = ObjectDetector.createFromOptions(context,
   ObjectDetector.ObjectDetectorOptions.builder().setBaseOptions(base)
    .setRunningMode(RunningMode.IMAGE).setScoreThreshold(0.1f).setMaxResults(10).build())
 }
 override fun detect(pixels: Bitmap): List<Detection> {
  val owned = pixels.copy(Bitmap.Config.ARGB_8888, false)
  val image = BitmapImageBuilder(owned).build()
  try {
   return detector.detect(image).detections().mapNotNull { d ->
    val c = d.categories().maxByOrNull { it.score() } ?: return@mapNotNull null
    val label = c.categoryName()
    if (label.isNullOrBlank() || label == "???") return@mapNotNull null
    val b = d.boundingBox()
    Detection(label,c.score(),Box(b.left,b.top,b.right,b.bottom))
   }
  } finally { image.close(); if (!owned.isRecycled) owned.recycle() }
 }
 override fun close() = detector.close()
 companion object { const val ASSET = "models/efficientdet_lite0_int8.tflite" }
}

class LocalTextReader : TextReader<Bitmap> {
 private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
 override fun read(pixels: Bitmap): List<OcrLine> {
  // Separate ownership: a timed-out ML Kit task may still reference its input.
  val copy = pixels.copy(Bitmap.Config.ARGB_8888, false)
  val task = recognizer.process(InputImage.fromBitmap(copy, 0))
  task.addOnCompleteListener(Executor { it.run() }) { copy.recycle() }
  val result = Tasks.await(task, 5, TimeUnit.SECONDS)
  return result.textBlocks.flatMap { b -> b.lines.map { line -> OcrLine(line.text, line.confidence) } }
 }
 override fun close() = recognizer.close()
}
