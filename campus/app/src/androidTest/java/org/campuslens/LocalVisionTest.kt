package org.campuslens
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.campuslens.inference.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class LocalVisionTest {
 @Test fun bundledDetectorLoadsAndRunsOnDevice() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val bitmap=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888)
  bitmap.eraseColor(Color.BLACK)
  LocalDetector(context).use { detector -> detector.detect(bitmap).forEach { assertTrue(it.confidence in 0f..1f) } }
  bitmap.recycle()
 }
 @Test fun bundledOcrReadsRenderedFixture() {
  val bitmap=Bitmap.createBitmap(1000,400,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE)
  Canvas(bitmap).drawText("ROOM 204",60f,200f,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.BLACK;textSize=100f })
  LocalTextReader().use { reader -> assertTrue(reader.read(bitmap).joinToString(" "){it.text}.contains("204")) }
  bitmap.recycle()
 }
}
