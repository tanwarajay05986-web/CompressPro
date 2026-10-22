package com.akay.compresspro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

object K2 {
    const val ARROW_THIN = 0
    const val SHARE = 1
}

// two more icons: a thin long arrow (login buttons) and the share icon
class Icon2View(ctx: Context, private val kind: Int, var color: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        fun x(v: Float): Float = ox + v * s
        fun y(v: Float): Float = oy + v * s
        p.color = color
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (kind) {
            K2.ARROW_THIN -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.065f * s
                c.drawLine(x(0.06f), y(0.5f), x(0.94f), y(0.5f), p)
                path.reset()
                path.moveTo(x(0.7f), y(0.26f))
                path.lineTo(x(0.94f), y(0.5f))
                path.lineTo(x(0.7f), y(0.74f))
                c.drawPath(path, p)
            }
            K2.SHARE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.07f * s
                c.drawLine(x(0.24f), y(0.5f), x(0.76f), y(0.22f), p)
                c.drawLine(x(0.24f), y(0.5f), x(0.76f), y(0.78f), p)
                p.style = Paint.Style.FILL
                c.drawCircle(x(0.78f), y(0.2f), 0.13f * s, p)
                c.drawCircle(x(0.22f), y(0.5f), 0.13f * s, p)
                c.drawCircle(x(0.78f), y(0.8f), 0.13f * s, p)
            }
        }
    }
}
