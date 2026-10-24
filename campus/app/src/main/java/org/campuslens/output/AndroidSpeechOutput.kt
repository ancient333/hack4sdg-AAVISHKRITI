package org.campuslens.output

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.campuslens.core.*
import java.util.Locale

class AndroidSpeechOutput(context: Context, private val onStatus: (String) -> Unit) : SpeechOutput {
 private val main = Handler(Looper.getMainLooper())
 private val policy = SpeechPolicy()
 private var tts: TextToSpeech? = null
 private var ready = false
 private var closed = false
 private var currentId = 0L
 private var lastSpoken: String? = null
 var mutedForCommand = false
 init {
  tts = TextToSpeech(context.applicationContext) { status -> main.post {
   if (closed) return@post
   val engine = tts ?: return@post
   if (status != TextToSpeech.SUCCESS) { onStatus("Speech unavailable. Check Android text-to-speech settings."); return@post }
   try {
    val voice = engine.voices?.filter { !it.isNetworkConnectionRequired && it.locale.language == "en" }
     ?.sortedBy { it.name }?.firstOrNull()
    if (voice == null || engine.setVoice(voice) != TextToSpeech.SUCCESS) {
     onStatus("No offline English voice installed. Download a voice in Android text-to-speech settings."); return@post
    }
    engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
     .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
     override fun onStart(id: String?) {}
     override fun onDone(id: String?) { main.post { if (id == currentId.toString()) advance() } }
     @Deprecated("Legacy callback") override fun onError(id: String?) {
      main.post { if (id == currentId.toString()) { onStatus("Speech playback failed. Check audio output."); advance() } }
     }
    })
    ready = true
    onStatus("Offline-capable English voice selected: ${voice.name}. Airplane-mode testing is still required.")
   } catch (e: Exception) { onStatus("Speech initialization failed: ${e.javaClass.simpleName}") }
  } }
 }
 override fun offer(event: SpeechEvent) {
  if (closed || !ready || mutedForCommand) return
  policy.offer(event,SystemClock.elapsedRealtime())?.let(::speak)
 }
 private fun speak(e: SpeechEvent) {
  currentId++
  val result = tts?.speak(e.text,TextToSpeech.QUEUE_FLUSH,null,currentId.toString())
  if (result == TextToSpeech.SUCCESS) lastSpoken = e.text
  else { policy.clear(); onStatus("Speech output failed. Check text-to-speech settings.") }
 }
 private fun advance() { if (!closed) policy.finished(SystemClock.elapsedRealtime())?.let(::speak) }
 override fun repeat() {
  val text = lastSpoken ?: "No spoken result is available yet."
  silence()
  offer(SpeechEvent("Previous result: $text",Priority.RESULT,SystemClock.elapsedRealtime(),30000))
  lastSpoken = text
 }
 override fun silence() { currentId++; policy.clear(); tts?.stop() }
 override fun close() { closed = true; silence(); ready = false; tts?.shutdown(); tts = null }
}
