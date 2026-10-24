package org.campuslens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.app.AlertDialog
import android.view.Gravity
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import org.campuslens.core.*
import org.campuslens.input.*
import org.campuslens.inference.*
import org.campuslens.output.AndroidSpeechOutput
import org.campuslens.ui.DetectionOverlay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

@androidx.camera.view.TransformExperimental
class MainActivity : ComponentActivity() {
 private val main = Handler(Looper.getMainLooper())
 private val worker = Executors.newSingleThreadExecutor()
 private val engine = AssistanceEngine()
 private val scenes = SceneDescriptions()
 private var lastSceneRequestMs = -1500L
 private val readRequested = AtomicBoolean(false)
 private val dropped = AtomicLong(0)
 private var detector: LocalDetector? = null // accessed only on worker
 private var reader: LocalTextReader? = null // accessed only on worker
 private lateinit var camera: PhoneCameraSource
 private lateinit var speech: AndroidSpeechOutput
 private lateinit var voice: AndroidVoiceCommands
 private lateinit var preview: PreviewView
 private lateinit var overlay: DetectionOverlay
 private lateinit var status: TextView
 private lateinit var speechStatus: TextView
 private lateinit var voiceStatus: TextView
 private lateinit var results: TextView
 private lateinit var metrics: TextView
 private lateinit var readButton: Button
 private lateinit var stopButton: Button
 private lateinit var startButton: Button
 private lateinit var voiceButton: Button
 private var visible = false
 private var closed = false
 @Volatile private var threshold = 0.55f
 private var lastResultMs = 0L
 private var startedMs = 0L
 private var lastDescriptionMs = 0L
 private var lastDistanceAnnouncementMs = 0L
 private var lastMetricsMs = 0L
 private var processed = 0
 private var errors = 0
 private val inferenceMs = ArrayDeque<Double>()
 private val decisionMs = ArrayDeque<Double>()
 private var thermal = 0
 private val thermalListener = PowerManager.OnThermalStatusChangedListener { thermal=it }
 private val ttsSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
  speech.close()
  speech=AndroidSpeechOutput(this) { speechStatus.text=it }
 }
 private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted && visible) startAssistance() else if (!granted) {
   system("Camera permission denied. Use App settings to allow camera access, then press Start.")
  }
 }
 private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted && visible) voice.listen() else if (!granted) system("Microphone permission denied. Touch controls remain available.")
 }
 private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
  val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
  if (granted && visible) locateCurrentPosition() else if (!granted) system("Location permission denied. No location was retrieved.")
 }
 private val watchdog = object : Runnable {
  override fun run() {
   if (closed) return
   val now=SystemClock.elapsedRealtime()
   if (engine.state == SessionState.STARTING && now-startedMs > 15000) fail("AI initialization timed out. Press Start to retry.")
   if (engine.state == SessionState.RUNNING) {
    if (now-lastResultMs > 1000) { overlay.marks=emptyList(); results.text="No fresh detection result." }
    if (now-lastResultMs > 10000) fail("Camera or AI processing stalled. Assistance stopped. Press Start to retry.")
   }
   main.postDelayed(this,500)
  }
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  buildUi()
  speech=AndroidSpeechOutput(this) { speechStatus.text=it }
  voice=AndroidVoiceCommands(this,::command,{ voiceStatus.text=it; speech.offer(event(it,Priority.RESULT)) },{ listening ->
   if (listening) speech.silence()
   speech.mutedForCommand=listening
   voiceButton.text=if (listening) "Cancel voice command" else "Voice command"
  })
  voiceStatus.text=if(voice.available()) "On-device recognizer available; English model readiness must be tested." else "On-device voice commands unavailable. Use touch controls."
  camera=PhoneCameraSource(this,this,preview,worker) { dropped.incrementAndGet() }
  getSystemService(PowerManager::class.java).addThermalStatusListener(thermalListener)
  main.post(watchdog)
  updateControls()
 }
 private fun buildUi() {
  val scroll=ScrollView(this).apply { setBackgroundColor(Color.rgb(16,24,32)); isFillViewport=true }
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(16),dp(16),dp(24)) }
  scroll.addView(root); setContentView(scroll)
  ViewCompat.setOnApplyWindowInsetsListener(scroll) { view,insets ->
   val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
   view.setPadding(bars.left,bars.top,bars.right,bars.bottom); insets
  }
  fun label(text: String, size: Float=18f): TextView = TextView(this).apply {
   this.text=text; textSize=size; setTextColor(Color.WHITE); setPadding(0,dp(8),0,dp(8)); root.addView(this)
  }
  val topBar=LinearLayout(this).apply {
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
  }
  val title=TextView(this).apply {
   text="Campus Lens"; textSize=28f; setTextColor(Color.WHITE)
   ViewCompat.setAccessibilityHeading(this,true)
  }
  topBar.addView(title,LinearLayout.LayoutParams(0,-2,1f))
  val menuButton=Button(this).apply {
   text="☰"; textSize=24f; contentDescription="Accessibility modes menu"
   isAllCaps=false; minWidth=dp(56); minHeight=dp(48)
   setOnClickListener { showModeMenu() }
  }
  topBar.addView(menuButton,LinearLayout.LayoutParams(-2,-2))
  root.addView(topBar,LinearLayout.LayoutParams(-1,-2))
  status=label("Stopped. Start assistance to use the phone camera.")
  status.accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
  fun button(text: String, action: () -> Unit): Button = Button(this).apply {
   this.text=text; textSize=18f; isAllCaps=false; minHeight=dp(56); setTextColor(Color.WHITE)
   setOnClickListener { action() }; root.addView(this,LinearLayout.LayoutParams(-1,-2))
  }
  startButton=button("Start assistance",::startAssistance)
  stopButton=button("Stop assistance") { stopAssistance(true) }
  readButton=button("Read text") { command(Command.READ) }
  button("Describe my surroundings") { command(Command.DESCRIBE) }
  button("Repeat last spoken result") { speech.repeat() }
  voiceButton=button("Voice command") {
   if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) voice.listen()
   else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
  }
  button("Stop speaking") { speech.silence() }
  val frame=FrameLayout(this)
  preview=PreviewView(this).apply { implementationMode=PreviewView.ImplementationMode.COMPATIBLE
   scaleType=PreviewView.ScaleType.FIT_CENTER; importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
  overlay=DetectionOverlay(this)
  frame.addView(preview,FrameLayout.LayoutParams(-1,-1));frame.addView(overlay,FrameLayout.LayoutParams(-1,-1))
  root.addView(frame,LinearLayout.LayoutParams(-1,dp(240)))
  results=label("No current detections.")
  speechStatus=label("Checking offline speech voice…",16f).apply { accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE }
  voiceStatus=label("Checking on-device voice commands…",16f).apply { accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE }
  val thresholdLabel=label("Detection confidence threshold: 55%",16f)
  root.addView(SeekBar(this).apply {
   max=80; progress=45; contentDescription="Detection confidence threshold, 10 to 90 percent"
   minHeight=dp(48)
   setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
     threshold=(value+10)/100f; thresholdLabel.text="Detection confidence threshold: ${value+10}%"
    }
    override fun onStartTrackingTouch(bar: SeekBar?) {}
    override fun onStopTrackingTouch(bar: SeekBar?) {}
   })
  })
  metrics=label("Performance: no frames processed yet.",14f)
  button("Copy performance report") {
   getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Campus Lens performance",metrics.text))
   system("Performance report copied.")
  }
  button("App permission settings") { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName"))) }
  button("Text-to-speech settings") {
   try { ttsSettings.launch(Intent("com.android.settings.TTS_SETTINGS")) }
   catch (_: Exception) { ttsSettings.launch(Intent(Settings.ACTION_SETTINGS)) }
  }
  label("Distance is an approximate camera-based estimate for supported object types, not a depth sensor. Do not rely on it for collision avoidance or safe navigation. OCR: English / Latin script.",16f)
 }

 private fun showModeMenu() {
  val options=arrayOf("Blind Mode (default)","Deaf Mode","Non-Speaking Mode")
  AlertDialog.Builder(this)
   .setTitle("Choose accessibility mode")
   .setItems(options) { dialog, which ->
    when(which) {
     0 -> { dialog.dismiss(); system("Blind Mode selected.") }
     1 -> showDeafMode()
     2 -> showNonSpeakingMode()
    }
   }
   .setNegativeButton("Cancel",null)
   .show()
 }
 private fun showDeafMode() {
  startActivity(Intent(this, DeafModeActivity::class.java))
 }
 private fun showNonSpeakingMode() {
  val input=EditText(this).apply { hint="Type a message to speak"; textSize=18f }
  AlertDialog.Builder(this).setTitle("Non-Speaking Mode")
   .setMessage("Type a message, then choose Speak.")
   .setView(input)
   .setPositiveButton("Speak") { _, _ ->
    val message=input.text.toString().trim()
    if(message.isNotEmpty()) speech.offer(event(message,Priority.RESULT,30000))
   }
   .setNegativeButton("Return",null).show()
 }

 private fun dp(v: Int)=(v*resources.displayMetrics.density).toInt()
 private fun event(text: String, priority: Priority=Priority.SYSTEM, ttl: Long=10000) =
  SpeechEvent(text,priority,SystemClock.elapsedRealtime(),ttl)
 private fun system(message: String) { status.text=message; speech.offer(event(message)) }
 private fun updateControls() {
  val active=engine.state==SessionState.RUNNING || engine.state==SessionState.STARTING
  startButton.isEnabled=!active; stopButton.isEnabled=active; readButton.isEnabled=engine.state==SessionState.RUNNING
 }
 private fun command(c: Command) { when(c) {
  Command.START -> startAssistance()
  Command.STOP -> stopAssistance(true)
  Command.READ -> {
   if (engine.state!=SessionState.RUNNING) system("Start assistance before reading text.")
   else if(readRequested.compareAndSet(false,true)) {
    status.text="Reading text from the next camera frame."
    speech.offer(event("Reading text.",Priority.RESULT))
   }
  }
  Command.DESCRIBE -> {
   val now=SystemClock.elapsedRealtime()
   // Repeated taps/commands reuse the current observation, never launch inference.
   if(now-lastSceneRequestMs >= 1500) {
    lastSceneRequestMs=now
    val description=scenes.describe(engine.generation,now,engine.state==SessionState.RUNNING,threshold)
    results.text=description.text
    speech.offer(description.speech(now,requested=true))
   }
  }
  Command.REPEAT -> speech.repeat()
  Command.LOCATE -> {
   val fine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
   val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
   if (!fine && !coarse) locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
   else locateCurrentPosition()
  }
 } }
 private fun locateCurrentPosition() {
  val fine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
  val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
  if (!fine && !coarse) { system("Location permission is required. Say where am I again after granting permission."); return }
  val manager=getSystemService(LocationManager::class.java)
  val provider=when {
   fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
   coarse && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
   fine && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
   else -> null
  }
  if (provider==null) { system("Location is unavailable. Turn on the phone location setting and try again."); return }
  system("Checking current phone location. This may take a few seconds.")
  try {
   manager.getCurrentLocation(provider,null,mainExecutor) { location: Location? ->
    if (closed || !visible) return@getCurrentLocation
    if (location==null) {
     system("Current location is unavailable. Move to an open area, check location settings, and try again.")
    } else {
     val accuracy=if(location.hasAccuracy()) " with approximate accuracy ${location.accuracy.toInt()} metres" else ""
     val message=String.format(java.util.Locale.US,"Current phone location: latitude %.5f, longitude %.5f%s. This is location information only, not a route or safety guidance.",location.latitude,location.longitude,accuracy)
     status.text=message
     speech.offer(event(message,Priority.RESULT,20000))
    }
   }
  } catch (_: SecurityException) {
   system("Location permission is missing. Grant location permission and try again.")
  } catch (_: Exception) {
   system("Could not retrieve current location. Check phone location settings and try again.")
  }
 }
 private fun startAssistance() {
  if (!visible || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
  if(engine.state==SessionState.RUNNING || engine.state==SessionState.STARTING) return
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
   cameraPermission.launch(Manifest.permission.CAMERA); return
  }
  scenes.clear();lastSceneRequestMs=-1500L
  val token=engine.start(); startedMs=SystemClock.elapsedRealtime(); updateControls(); system("Loading local assistance models.")
  worker.execute {
   try {
    closeModels()
    detector=LocalDetector(applicationContext); reader=LocalTextReader()
    main.post {
     if (closed || !visible || token!=engine.generation) return@post
     engine.ready(token); startedMs=SystemClock.elapsedRealtime();lastResultMs=startedMs
     processed=0;errors=0;dropped.set(0);inferenceMs.clear();decisionMs.clear();lastDescriptionMs=0
     updateControls();window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
     camera.start({ packet -> processFrame(packet,token) },::fail)
     system("Assistance started. Waiting for camera observations.")
    }
   } catch(e: LinkageError) {
    closeModels()
    main.post { if(!closed && token==engine.generation) fail("Local AI runtime incompatible with this device. See the build report.") }
   } catch(e: Exception) {
    closeModels()
    main.post { if(!closed && token==engine.generation) fail("Local AI models unavailable: ${e.javaClass.simpleName}. Reinstall the complete app or retry.") }
   }
  }
 }
 private fun processFrame(packet: FramePacket<android.graphics.Bitmap>, token: Long) {
  val frame=packet as? BitmapFrame ?: return
  val begin=SystemClock.elapsedRealtimeNanos()
  try {
   val items=DetectionPolicy.filter(detector?.detect(frame.pixels) ?: error("Detector not ready"),threshold)
   val inference=(SystemClock.elapsedRealtimeNanos()-begin)/1e6
   main.post {
    val now=SystemClock.elapsedRealtime()
    if (!engine.accepts(token,frame.metadata.receivedNs/1_000_000,now)) { dropped.incrementAndGet(); return@post }
    scenes.observe(items,token,frame.metadata.receivedNs/1_000_000)
    lastResultMs=now;processed++
    if(processed==1) status.text="Assistance active. Phone camera and local AI are processing."
    sample(inferenceMs,inference);sample(decisionMs,(SystemClock.elapsedRealtimeNanos()-frame.metadata.receivedNs)/1e6)
    val rangedItems = items.map { detection ->
     detection to ApproximateDistanceEstimator.estimateMetres(detection, frame.pixels.width)
    }
    val transform=preview.outputTransform
    overlay.marks=if(transform==null) emptyList() else try {
     val mapping=CoordinateTransform(frame.previewTransform,transform)
     rangedItems.map { (d, distance) ->
      val b=d.box;val rect=RectF(b.left,b.top,b.right,b.bottom);mapping.mapRect(rect)
      val rangeText = distance?.let { " ~${it}m" } ?: ""
      DetectionOverlay.Mark(rect,"${d.label} ${(d.confidence*100).toInt()}%$rangeText")
     }
    } catch(_: IllegalArgumentException) { emptyList() }
    results.text=if(items.isEmpty()) "No objects recognized above the threshold. This does not mean the view is clear."
     else rangedItems.joinToString(" • ") { (d, distance) ->
      "${d.label} ${(d.confidence*100).toInt()}%${distance?.let { " — about ${it} m away" } ?: " — distance unavailable"}"
     }
    val nearestEstimate = rangedItems.filter { it.second != null }.minByOrNull { it.second!! }
    if (nearestEstimate != null && now - lastDistanceAnnouncementMs >= 15000L) {
     val distance = nearestEstimate.second!!
     speech.offer(event("${nearestEstimate.first.label}, approximately ${ApproximateDistanceEstimator.spokenDistance(distance)} away. Distance is an estimate.",Priority.RESULT,5000))
     lastDistanceAnnouncementMs = now
    }
    if(now-lastDescriptionMs>=15000L) {
     val description=scenes.describe(token,now,true,threshold)
     // Empty results are explained on demand, not repeated automatically.
     if(items.isNotEmpty()) speech.offer(description.speech(now,requested=false))
     lastDescriptionMs=now
    }
    if(now-lastMetricsMs>=2000) { updateMetrics(now);lastMetricsMs=now }
   }
   if(readRequested.getAndSet(false)) {
    try {
     val message=OcrPolicy.message(reader?.read(frame.pixels) ?: error("OCR not ready"))
     main.post {
      if(!closed && token==engine.generation && engine.state==SessionState.RUNNING) {
       status.text="Text result: $message";speech.offer(event(message,Priority.RESULT,30000))
      }
     }
    } catch(e: Exception) { main.post { if(!closed && token==engine.generation) {
     errors++;system("Text reading failed. Hold the camera steady and try again.")
    } } }
   }
  } catch(e: LinkageError) { main.post { if(!closed && token==engine.generation) fail("AI runtime unavailable. Assistance stopped.") }
  } catch(e: Exception) { main.post { if(!closed && token==engine.generation) {
   errors++;fail("Object detection failed: ${e.javaClass.simpleName}. Press Start to retry.")
  } } }
 }
 private fun sample(queue: ArrayDeque<Double>, value: Double) { if(queue.size>=240) queue.removeFirst();queue.addLast(value) }
 private fun p95(queue: ArrayDeque<Double>): Double = if(queue.isEmpty()) 0.0 else queue.sorted()[(ceil(queue.size*.95).toInt()-1).coerceAtLeast(0)]
 private fun updateMetrics(now: Long) {
  val fps=processed*1000.0/(now-startedMs).coerceAtLeast(1)
  val battery=getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
  metrics.text=String.format(java.util.Locale.US,
   "Processed %d | %.1f FPS | inference p95 %.1f ms | arrival-to-decision p95 %.1f ms | app drops %d | errors %d | battery %d%% | thermal %d\nLast 240 timing samples. CameraX internal drops not counted. Sensor capture latency not measured.",
   processed,fps,p95(inferenceMs),p95(decisionMs),dropped.get(),errors,battery,thermal)
  Log.i("CampusLensMetrics",metrics.text.toString())
 }
 private fun stopAssistance(announce: Boolean) {
  scenes.clear();lastSceneRequestMs=-1500L
  engine.stop();readRequested.set(false);camera.stop();speech.silence()
  overlay.marks=emptyList();results.text="No current detections.";updateControls()
  window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  worker.execute { closeModels() }
  if(announce) system("Assistance stopped.") else status.text="Paused. Press Start to resume assistance."
 }
 private fun fail(message: String) {
  voice.cancel()
  stopAssistance(false);engine.fail();updateControls();system(message)
 }
 private fun closeModels() {
  try { detector?.close() } catch(_: Exception) {} finally { detector=null }
  try { reader?.close() } catch(_: Exception) {} finally { reader=null }
 }
 override fun onStart() { super.onStart();visible=true }
 override fun onStop() {
  visible=false;voice.cancel();stopAssistance(false);super.onStop()
 }
 override fun onDestroy() {
  closed=true;main.removeCallbacks(watchdog);voice.close();speech.close()
  getSystemService(PowerManager::class.java).removeThermalStatusListener(thermalListener)
  worker.shutdown();super.onDestroy()
 }
}
