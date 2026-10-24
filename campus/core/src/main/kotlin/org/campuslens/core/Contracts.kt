package org.campuslens.core

data class FrameMetadata(val source: String, val capturedNs: Long, val receivedNs: Long,
                         val width: Int, val height: Int, val rotationDegrees: Int) {
 init { require(source.isNotBlank()); require(width > 0 && height > 0)
  require(capturedNs >= 0 && receivedNs >= 0); require(rotationDegrees in listOf(0,90,180,270)) }
}
// Pixel ownership belongs to the consumer until close(). No Android camera type crosses this boundary.
interface FramePacket<T> : AutoCloseable { val metadata: FrameMetadata; val pixels: T }
interface FrameSource<T> { fun start(onFrame: (FramePacket<T>) -> Unit, onError: (String) -> Unit); fun stop() }
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
 init { require(listOf(left,top,right,bottom).all { it.isFinite() }); require(right >= left && bottom >= top) }
}
data class Detection(val label: String, val confidence: Float, val box: Box) {
 init { require(label.isNotBlank()); require(confidence.isFinite() && confidence in 0f..1f) }
}
interface Detector<T> : AutoCloseable { fun detect(pixels: T): List<Detection> }
data class OcrLine(val text: String, val confidence: Float?)
interface TextReader<T> : AutoCloseable { fun read(pixels: T): List<OcrLine> }
enum class Priority { DESCRIPTION, RESULT, SYSTEM }
data class SpeechEvent(val text: String, val priority: Priority, val createdMs: Long, val ttlMs: Long = 10000, val allowRepeat: Boolean = false)
interface SpeechOutput : AutoCloseable { fun offer(event: SpeechEvent); fun repeat(); fun silence() }
enum class Command { START, STOP, READ, REPEAT, DESCRIBE, LOCATE }
interface VoiceCommands : AutoCloseable { fun listen(); fun cancel() }
object CommandParser {
 fun parse(text: String): Command? = when(text.lowercase().trim().replace(Regex("[.!?]"), "")) {
  "start", "start assistance" -> Command.START
  "stop", "stop assistance" -> Command.STOP
  "read", "read text" -> Command.READ
  "describe", "describe surroundings", "describe my surroundings", "what can you see" -> Command.DESCRIBE
  "repeat", "repeat the last result", "repeat last result" -> Command.REPEAT
  "where am i", "current location", "my location", "find my location", "tell me my location" -> Command.LOCATE
  else -> null
 }
}
