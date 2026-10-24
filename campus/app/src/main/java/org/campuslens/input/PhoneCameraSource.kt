package org.campuslens.input

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.campuslens.core.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

@androidx.camera.view.TransformExperimental
class BitmapFrame(override val metadata: FrameMetadata, override val pixels: Bitmap,
                  val previewTransform: OutputTransform) : FramePacket<Bitmap> {
 private val closed = AtomicBoolean(false)
 override fun close() { if (closed.compareAndSet(false,true)) pixels.recycle() }
}

@androidx.camera.view.TransformExperimental
class PhoneCameraSource(private val context: Context, private val owner: LifecycleOwner,
 private val previewView: PreviewView, private val worker: ExecutorService,
 private val onDrop: () -> Unit) : FrameSource<Bitmap> {
 private var provider: ProcessCameraProvider? = null
 private var analysis: ImageAnalysis? = null
 private var camera: Camera? = null
 @Volatile private var generation = 0
 @Volatile private var running = false
 private var previousNs = 0L
 override fun start(onFrame: (FramePacket<Bitmap>) -> Unit, onError: (String) -> Unit) {
  stop(); running = true; val token = generation
  val future = ProcessCameraProvider.getInstance(context)
  future.addListener({
   if (!running || token != generation) return@addListener
   try {
    provider = future.get()
    if (!provider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) error("Rear camera unavailable")
    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
    val preview = Preview.Builder().setTargetRotation(rotation).build()
    preview.setSurfaceProvider(previewView.surfaceProvider)
    val a = ImageAnalysis.Builder().setTargetRotation(rotation)
     .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
     .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
     .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
      ResolutionStrategy(Size(1280,720),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
     .build()
    analysis = a
    a.setAnalyzer(worker) { proxy ->
     var frame: BitmapFrame? = null
     try {
      val now = SystemClock.elapsedRealtimeNanos()
      if (!running || token != generation || now - previousNs < 125_000_000L) { onDrop(); return@setAnalyzer }
      previousNs = now
      val transform = ImageProxyTransformFactory().apply {
       isUsingRotationDegrees = true; isUsingCropRect = true
      }.getOutputTransform(proxy)
      val raw = proxy.toBitmap()
      val degrees = proxy.imageInfo.rotationDegrees
      val crop = proxy.cropRect
      val upright = Bitmap.createBitmap(raw,crop.left,crop.top,crop.width(),crop.height(),
       Matrix().apply { postRotate(degrees.toFloat()) },true).also { if (it !== raw) raw.recycle() }
      frame = BitmapFrame(FrameMetadata("phone-rear",proxy.imageInfo.timestamp,now,
       upright.width,upright.height,0),upright,transform)
      // Consumer must finish synchronously; it owns the copied bitmap, not ImageProxy.
      onFrame(frame)
     } catch (e: Exception) {
      if (running && token == generation) ContextCompat.getMainExecutor(context).execute {
       if (running && token == generation) onError("Camera processing failed: ${e.javaClass.simpleName}")
      }
     } finally { frame?.close(); proxy.close() }
    }
    val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(a)
    previewView.viewPort?.let { group.setViewPort(it) }
    camera = provider!!.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,group.build())
    camera!!.cameraInfo.cameraState.observe(owner) { state ->
     if (running && token == generation && state.error != null) onError("Camera unavailable. Close other camera apps and retry.")
    }
   } catch (e: Exception) { onError("Could not open the camera: ${e.javaClass.simpleName}") }
  },ContextCompat.getMainExecutor(context))
 }
 override fun stop() {
  running = false; generation++
  camera?.cameraInfo?.cameraState?.removeObservers(owner); camera = null
  analysis?.clearAnalyzer(); analysis = null
  provider?.unbindAll()
 }
}
