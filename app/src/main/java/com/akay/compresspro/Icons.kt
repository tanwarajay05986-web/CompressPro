package com.akay.compresspro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

object K {
    const val ARROW_L = 0
    const val CHEV_R = 1
    const val HOME = 2
    const val GEAR = 3
    const val CROWN = 4
    const val IMAGE = 5
    const val PALETTE = 6
    const val GLOBE = 7
    const val SHIELD = 8
    const val FOLDER = 9
    const val INFO = 10
    const val DOC = 11
    const val STAR = 12
    const val RESIZE = 13
}

// all small icons are drawn in code
class IconView(ctx: Context, private val kind: Int, private val color: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    private fun darker(c: Int): Int =
        Color.rgb((Color.red(c) * 0.55f).toInt(), (Color.green(c) * 0.55f).toInt(), (Color.blue(c) * 0.55f).toInt())

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        fun x(v: Float): Float = ox + v * s
        fun y(v: Float): Float = oy + v * s

        p.style = Paint.Style.FILL
        p.color = color
        p.shader = null
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 0.09f * s

        when (kind) {
            K.ARROW_L -> {
                p.style = Paint.Style.STROKE
                c.drawLine(x(0.82f), y(0.5f), x(0.2f), y(0.5f), p)
                path.reset()
                path.moveTo(x(0.46f), y(0.22f))
                path.lineTo(x(0.2f), y(0.5f))
                path.lineTo(x(0.46f), y(0.78f))
                c.drawPath(path, p)
            }
            K.CHEV_R -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.1f * s
                path.reset()
                path.moveTo(x(0.36f), y(0.2f))
                path.lineTo(x(0.66f), y(0.5f))
                path.lineTo(x(0.36f), y(0.8f))
                c.drawPath(path, p)
            }
            K.HOME -> {
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                path.moveTo(x(0.5f), y(0.12f))
                path.lineTo(x(0.92f), y(0.5f))
                path.lineTo(x(0.8f), y(0.5f))
                path.lineTo(x(0.8f), y(0.88f))
                path.lineTo(x(0.2f), y(0.88f))
                path.lineTo(x(0.2f), y(0.5f))
                path.lineTo(x(0.08f), y(0.5f))
                path.close()
                path.addRect(x(0.42f), y(0.6f), x(0.58f), y(0.88f), Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
            }
            K.GEAR -> {
                c.save()
                c.translate(x(0.5f), y(0.5f))
                for (i in 0 until 8) {
                    rf.set(-0.09f * s, -0.46f * s, 0.09f * s, -0.28f * s)
                    c.drawRoundRect(rf, 0.03f * s, 0.03f * s, p)
                    c.rotate(45f)
                }
                c.restore()
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                path.addCircle(x(0.5f), y(0.5f), 0.34f * s, Path.Direction.CW)
                path.addCircle(x(0.5f), y(0.5f), 0.14f * s, Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
            }
            K.CROWN -> {
                path.reset()
                path.moveTo(x(0.14f), y(0.78f))
                path.lineTo(x(0.08f), y(0.3f))
                path.lineTo(x(0.32f), y(0.52f))
                path.lineTo(x(0.5f), y(0.2f))
                path.lineTo(x(0.68f), y(0.52f))
                path.lineTo(x(0.92f), y(0.3f))
                path.lineTo(x(0.86f), y(0.78f))
                path.close()
                c.drawPath(path, p)
                c.drawCircle(x(0.08f), y(0.27f), 0.06f * s, p)
                c.drawCircle(x(0.5f), y(0.17f), 0.06f * s, p)
                c.drawCircle(x(0.92f), y(0.27f), 0.06f * s, p)
                rf.set(x(0.15f), y(0.82f), x(0.85f), y(0.9f))
                c.drawRoundRect(rf, 0.03f * s, 0.03f * s, p)
            }
            K.IMAGE -> {
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                rf.set(x(0.1f), y(0.12f), x(0.9f), y(0.88f))
                path.addRoundRect(rf, 0.16f * s, 0.16f * s, Path.Direction.CW)
                path.moveTo(x(0.2f), y(0.74f))
                path.lineTo(x(0.4f), y(0.46f))
                path.lineTo(x(0.54f), y(0.64f))
                path.lineTo(x(0.64f), y(0.52f))
                path.lineTo(x(0.8f), y(0.74f))
                path.close()
                path.addCircle(x(0.7f), y(0.32f), 0.07f * s, Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
            }
            K.PALETTE -> {
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                path.addCircle(x(0.5f), y(0.5f), 0.4f * s, Path.Direction.CW)
                path.addCircle(x(0.64f), y(0.74f), 0.09f * s, Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
                val cols = intArrayOf(0xFFFF6B6B.toInt(), 0xFFFFD166.toInt(), 0xFF4ADE80.toInt(), 0xFF3B82F6.toInt())
                val px = floatArrayOf(0.32f, 0.5f, 0.7f, 0.24f)
                val py = floatArrayOf(0.4f, 0.27f, 0.38f, 0.6f)
                for (i in 0 until 4) {
                    p.color = cols[i]
                    c.drawCircle(x(px[i]), y(py[i]), 0.075f * s, p)
                }
            }
            K.GLOBE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.06f * s
                c.drawCircle(x(0.5f), y(0.5f), 0.38f * s, p)
                rf.set(x(0.33f), y(0.12f), x(0.67f), y(0.88f))
                c.drawOval(rf, p)
                c.drawLine(x(0.12f), y(0.5f), x(0.88f), y(0.5f), p)
                c.drawLine(x(0.18f), y(0.3f), x(0.82f), y(0.3f), p)
                c.drawLine(x(0.18f), y(0.7f), x(0.82f), y(0.7f), p)
            }
            K.SHIELD -> {
                path.reset()
                path.moveTo(x(0.5f), y(0.08f))
                path.lineTo(x(0.86f), y(0.22f))
                path.lineTo(x(0.86f), y(0.5f))
                path.quadTo(x(0.86f), y(0.8f), x(0.5f), y(0.94f))
                path.quadTo(x(0.14f), y(0.8f), x(0.14f), y(0.5f))
                path.lineTo(x(0.14f), y(0.22f))
                path.close()
                c.drawPath(path, p)
                p.color = darker(color)
                path.reset()
                path.moveTo(x(0.5f), y(0.08f))
                path.lineTo(x(0.86f), y(0.22f))
                path.lineTo(x(0.86f), y(0.5f))
                path.quadTo(x(0.86f), y(0.8f), x(0.5f), y(0.94f))
                path.close()
                c.drawPath(path, p)
            }
            K.FOLDER -> {
                path.reset()
                path.moveTo(x(0.08f), y(0.24f))
                path.lineTo(x(0.38f), y(0.24f))
                path.lineTo(x(0.48f), y(0.36f))
                path.lineTo(x(0.92f), y(0.36f))
                path.lineTo(x(0.92f), y(0.8f))
                path.lineTo(x(0.08f), y(0.8f))
                path.close()
                c.drawPath(path, p)
                p.color = darker(color)
                rf.set(x(0.08f), y(0.44f), x(0.92f), y(0.49f))
                c.drawRect(rf, p)
            }
            K.INFO -> {
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                path.addCircle(x(0.5f), y(0.5f), 0.42f * s, Path.Direction.CW)
                path.addCircle(x(0.5f), y(0.3f), 0.06f * s, Path.Direction.CW)
                rf.set(x(0.45f), y(0.42f), x(0.55f), y(0.74f))
                path.addRoundRect(rf, 0.04f * s, 0.04f * s, Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
            }
            K.DOC -> {
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                rf.set(x(0.2f), y(0.08f), x(0.8f), y(0.92f))
                path.addRoundRect(rf, 0.1f * s, 0.1f * s, Path.Direction.CW)
                path.addRect(x(0.32f), y(0.3f), x(0.68f), y(0.37f), Path.Direction.CW)
                path.addRect(x(0.32f), y(0.47f), x(0.68f), y(0.54f), Path.Direction.CW)
                path.addRect(x(0.32f), y(0.64f), x(0.52f), y(0.71f), Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
            }
            K.STAR -> {
                path.reset()
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) 0.42f else 0.19f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val px = x(0.5f) + (rad * s * Math.cos(a)).toFloat()
                    val py = y(0.55f) + (rad * s * Math.sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.06f * s
                c.drawPath(path, p)
            }
            K.RESIZE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 0.1f * s
                c.drawLine(x(0.3f), y(0.7f), x(0.7f), y(0.3f), p)
                path.reset()
                path.moveTo(x(0.46f), y(0.3f))
                path.lineTo(x(0.7f), y(0.3f))
                path.lineTo(x(0.7f), y(0.54f))
                c.drawPath(path, p)
                path.reset()
                path.moveTo(x(0.3f), y(0.46f))
                path.lineTo(x(0.3f), y(0.7f))
                path.lineTo(x(0.54f), y(0.7f))
                c.drawPath(path, p)
            }
        }
    }
}
