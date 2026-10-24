package org.campuslens.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class DetectionOverlay(context: Context) : View(context) {
 data class Mark(val bounds: RectF, val label: String)
 var marks: List<Mark> = emptyList(); set(value) { field=value; invalidate() }
 private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.YELLOW; style=Paint.Style.STROKE; strokeWidth=3f }
 private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE; textSize=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,16f,resources.displayMetrics) }
 private val background = Paint().apply { color=Color.BLACK }
 init { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
 override fun onDraw(canvas: Canvas) {
  super.onDraw(canvas)
  for (m in marks) {
   canvas.drawRect(m.bounds,line)
   val x=m.bounds.left.coerceAtLeast(0f); val y=(m.bounds.top-6).coerceAtLeast(text.textSize+4)
   canvas.drawRect(x,y-text.textSize,x+text.measureText(m.label)+8,y+5,background)
   canvas.drawText(m.label,x+4,y,text)
  }
 }
}
