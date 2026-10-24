package org.campuslens.core

/** Snapshot of one detector result, never a combination of different frames. UI-thread owned. */
class SceneDescriptions(private val maxAgeMs: Long = 1000) {
 private data class Observation(val detections: List<Detection>, val session: Long, val receivedMs: Long)
 private var latest: Observation? = null
 fun observe(detections: List<Detection>, session: Long, receivedMs: Long) {
  latest = Observation(detections.toList(),session,receivedMs)
 }
 fun clear() { latest = null }
 fun describe(session: Long, nowMs: Long, available: Boolean, threshold: Float): SceneReply {
  if (!available) return SceneReply("Scene assistance is unavailable. Press Start assistance to try again.",null)
  val observation = latest
  if (observation == null || observation.session != session || nowMs < observation.receivedMs ||
   nowMs-observation.receivedMs > maxAgeMs) {
   return SceneReply("No recent camera observation is available. Please try again.",null)
  }
  val reliable = DetectionPolicy.filter(observation.detections,threshold)
  return SceneReply(render(reliable),observation.receivedMs)
 }
 private fun render(detections: List<Detection>): String {
  val groups=detections.groupingBy { it.label.trim().lowercase(java.util.Locale.ROOT) }.eachCount()
   .entries.sortedWith(compareByDescending<Map.Entry<String,Int>> { it.value }.thenBy { it.key }).take(3)
  if(groups.isEmpty()) return "I could not identify the surroundings clearly."
  val parts=groups.map { (label,count) ->
   if(count==1) when(label) {
    "scissors", "skis" -> label
    else -> "${if(label.first() in "aeiou") "an" else "a"} $label"
   } else "${numbers.getOrNull(count) ?: count.toString()} ${plural(label)}"
  }
  val joined=if(parts.size==1) parts.single() else parts.dropLast(1).joinToString(", ")+" and "+parts.last()
  return "I can identify $joined."
 }
 private fun plural(label: String): String = when(label) {
  "person" -> "people"
  "mouse" -> "mice"
  "knife" -> "knives"
  "sheep", "scissors", "skis" -> label
  else -> when {
   label.endsWith("s") || label.endsWith("ch") || label.endsWith("sh") || label.endsWith("x") -> label+"es"
   label.endsWith("y") && label.length>1 && label[label.length-2] !in "aeiou" -> label.dropLast(1)+"ies"
   else -> label+"s"
  }
 }
 companion object { private val numbers=listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten") }
}

data class SceneReply(val text: String, val observedMs: Long?) {
 /** Observation age travels into the speech queue; waiting does not make a scene fresh again. */
 fun speech(nowMs: Long, requested: Boolean): SpeechEvent = SpeechEvent(
  text,if(requested) Priority.RESULT else Priority.DESCRIPTION,observedMs ?: nowMs,
  if(observedMs == null) 3000 else 1000,allowRepeat=requested)
}
