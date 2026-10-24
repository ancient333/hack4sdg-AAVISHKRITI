package org.campuslens.core

enum class SessionState { STOPPED, STARTING, RUNNING, ERROR }
class AssistanceEngine {
 var state = SessionState.STOPPED; private set
 var generation = 0L; private set
 fun start(): Long { generation++; state = SessionState.STARTING; return generation }
 fun ready(token: Long) { if (token == generation && state == SessionState.STARTING) state = SessionState.RUNNING }
 fun stop() { generation++; state = SessionState.STOPPED }
 fun fail() { generation++; state = SessionState.ERROR }
 fun accepts(token: Long, receivedMs: Long, nowMs: Long): Boolean = token == generation &&
  state == SessionState.RUNNING && nowMs >= receivedMs && nowMs - receivedMs <= 1000
}
object DetectionPolicy {
 fun filter(items: List<Detection>, threshold: Float): List<Detection> {
  require(threshold.isFinite() && threshold in 0f..1f)
  return items.filter { it.confidence >= threshold }.sortedByDescending { it.confidence }
 }
 fun describe(items: List<Detection>): String? = items.map { it.label }.distinct().take(3)
  .takeIf { it.isNotEmpty() }?.joinToString(prefix="In the camera view: ", postfix=".")
}
object OcrPolicy {
 fun message(lines: List<OcrLine>): String {
  val readable = lines.filter { it.confidence == null || (it.confidence.isFinite() && it.confidence >= 0.5f) }
   .map { it.text.trim() }.filter { it.isNotEmpty() }
  if (readable.isEmpty()) return "I could not read clear text. Hold the camera steady and try again."
  val text = readable.joinToString(" ")
  return if (text.length > 1800) text.take(1800) + ". End of this reading. Reframe the remaining text to continue." else text
 }
}
// Called on a single UI thread. Keeps at most one waiting event per priority.
class SpeechPolicy(private val duplicateWindowMs: Long = 20000) {
 private val last = mutableMapOf<String, Long>()
 private val waiting = mutableMapOf<Priority, SpeechEvent>()
 var active: SpeechEvent? = null; private set
 fun offer(e: SpeechEvent, now: Long): SpeechEvent? {
  if (now - e.createdMs > e.ttlMs || e.text.isBlank()) return null
  last.entries.removeAll { now - it.value >= duplicateWindowMs }
  if (active?.text == e.text || (!e.allowRepeat && last.containsKey(e.text))) return null
  if (active == null || e.priority.ordinal > active!!.priority.ordinal) {
   if (e.priority == Priority.SYSTEM) waiting.clear()
   active = e; last[e.text] = now; return e
  }
  waiting[e.priority] = e
  return null
 }
 fun finished(now: Long): SpeechEvent? {
  active = null
  val next = waiting.values.filter { now - it.createdMs <= it.ttlMs }
   .maxByOrNull { it.priority.ordinal }
  waiting.entries.removeAll { now - it.value.createdMs > it.value.ttlMs }
  if (next != null) { waiting.remove(next.priority); active = next; last[next.text] = now }
  return next
 }
 fun clear() { active = null; waiting.clear() }
}
object ModelAssetValidator {
 fun requireTflite(bytes: ByteArray) {
  require(bytes.size > 1024 && bytes.copyOfRange(4,8).contentEquals("TFL3".toByteArray())) {
   "Bundled detector missing or invalid"
  }
 }
}
