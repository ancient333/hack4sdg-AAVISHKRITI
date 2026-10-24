package org.campuslens.input

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import org.campuslens.core.*

class AndroidVoiceCommands(private val context: Context, private val onCommand: (Command) -> Unit,
 private val onStatus: (String) -> Unit, private val onListening: (Boolean) -> Unit) : VoiceCommands {
 private var recognizer: SpeechRecognizer? = null
 private var listening = false
 private var generation = 0L
 private val main = Handler(Looper.getMainLooper())
 private val timeout = Runnable {
  if (listening) { cancel(); onStatus("Voice command timed out. Try again or use touch controls.") }
 }
 fun available() = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
 override fun listen() {
  if (!available()) { onStatus("On-device voice recognition unavailable. Use the touch controls."); return }
  if (listening) { cancel(); return }
  val token = ++generation
  try {
   val r = recognizer ?: SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { recognizer = it }
   r.setRecognitionListener(object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) { if(token == generation && listening) onStatus("Listening. Say start assistance, stop assistance, read text, describe my surroundings, repeat, or where am I.") }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { if(token == generation && listening) onStatus("Processing voice command on device.") }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onError(error: Int) {
     if(token != generation || !listening) return
     main.removeCallbacks(timeout)
     listening = false; onListening(false)
     onStatus("Voice command unavailable or not understood (code $error). Use touch controls or check downloaded English recognition support.")
    }
    override fun onResults(results: Bundle?) {
     if(token != generation || !listening) return
     main.removeCallbacks(timeout)
     listening = false; onListening(false)
     val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
     val command = texts.firstNotNullOfOrNull { CommandParser.parse(it) }
     if (command == null) onStatus("Command not recognized. Say start assistance, stop assistance, read text, describe my surroundings, repeat, or where am I.")
     else onCommand(command)
    }
   })
   listening = true; onListening(true); main.postDelayed(timeout,12000)
   r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US")
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
   })
  } catch (e: Exception) { main.removeCallbacks(timeout); listening = false; onListening(false); onStatus("Voice recognition failed. Use touch controls.") }
 }
 override fun cancel() { generation++; main.removeCallbacks(timeout); listening = false; recognizer?.cancel(); onListening(false) }
 override fun close() { cancel(); recognizer?.destroy(); recognizer = null }
}
