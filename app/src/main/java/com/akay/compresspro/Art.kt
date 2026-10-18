package com.akay.compresspro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View

// the picture on the home screen: a blue image card, a red PDF card and two arrows
class HeroArt(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    init {
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = Paint.Align.CENTER
        tp.color = Color.WHITE
    }

    private fun arrow(c: Canvas, cx: Float, cy: Float, r: Float, start: Float, sweep: Float, col: Int, sw: Float) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.strokeCap = Paint.Cap.ROUND
        p.color = col
        rf.set(cx - r, cy - r, cx + r, cy + r)
        c.drawArc(rf, start, sweep, false, p)
        val a = Math.toRadians((start + sweep).toDouble())
        val ex = cx + (r * Math.cos(a)).toFloat()
        val ey = cy + (r * Math.sin(a)).toFloat()
        val rx = Math.cos(a).toFloat()
        val ry = Math.sin(a).toFloat()
        val tx = -ry
        val ty = rx
        val hl = sw * 2.2f
        val hw = sw * 1.5f
        path.reset()
        path.moveTo(ex + tx * hl, ey + ty * hl)
        path.lineTo(ex + rx * hw, ey + ry * hw)
        path.lineTo(ex - rx * hw, ey - ry * hw)
        path.close()
        p.style = Paint.Style.FILL
        c.drawPath(path, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // blue image card
        c.save()
        c.rotate(-9f, w * 0.36f, h * 0.42f)
        rf.set(w * 0.06f, h * 0.12f, w * 0.66f, h * 0.76f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            rf.left, rf.top, rf.right, rf.bottom,
            0xFF1FD0FF.toInt(), 0xFF2F6BFF.toInt(), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rf, w * 0.07f, w * 0.07f, p)
        p.shader = null
        rf.inset(w * 0.05f, w * 0.05f)
        p.color = 0x33FFFFFF
        c.drawRoundRect(rf, w * 0.045f, w * 0.045f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.025f
        p.color = 0xE6FFFFFF.toInt()
        c.drawRoundRect(rf, w * 0.045f, w * 0.045f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFF1E6BFF.toInt()
        path.reset()
        val l = rf.left
        val t = rf.top
        val rw = rf.width()
        val rh = rf.height()
        path.moveTo(l + rw * 0.06f, t + rh * 0.92f)
        path.lineTo(l + rw * 0.3f, t + rh * 0.45f)
        path.lineTo(l + rw * 0.5f, t + rh * 0.72f)
        path.lineTo(l + rw * 0.68f, t + rh * 0.42f)
        path.lineTo(l + rw * 0.94f, t + rh * 0.92f)
        path.close()
        c.drawPath(path, p)
        p.color = Color.WHITE
        c.drawCircle(l + rw * 0.7f, t + rh * 0.22f, w * 0.04f, p)
        c.restore()

        // red PDF card
        c.save()
        c.rotate(10f, w * 0.74f, h * 0.68f)
        rf.set(w * 0.55f, h * 0.38f, w * 0.93f, h * 0.94f)
        p.shader = LinearGradient(
            rf.left, rf.top, rf.right, rf.bottom,
            0xFFFF4D7A.toInt(), 0xFFE01B50.toInt(), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rf, w * 0.05f, w * 0.05f, p)
        p.shader = null
        val cs = w * 0.1f
        path.reset()
        path.moveTo(rf.right - cs, rf.top)
        path.lineTo(rf.right, rf.top + cs)
        path.lineTo(rf.right - cs, rf.top + cs)
        path.close()
        p.color = 0xCCFFFFFF.toInt()
        c.drawPath(path, p)
        tp.textSize = w * 0.11f
        c.drawText("PDF", rf.centerX(), rf.centerY() + w * 0.04f, tp)
        c.restore()

        arrow(c, w * 0.8f, h * 0.2f, w * 0.1f, 200f, 150f, 0xFF7B3DFF.toInt(), w * 0.03f)
        arrow(c, w * 0.3f, h * 0.88f, w * 0.09f, 20f, 150f, 0xFF19D3C5.toInt(), w * 0.03f)
    }
}

// red PDF document used in the PDF card
class PdfGlyph(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = Paint.Align.CENTER
        tp.color = Color.WHITE
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        fun x(v: Float): Float = ox + v * s
        fun y(v: Float): Float = oy + v * s
        path.reset()
        path.moveTo(x(0.2f), y(0.06f))
        path.lineTo(x(0.62f), y(0.06f))
        path.lineTo(x(0.86f), y(0.3f))
        path.lineTo(x(0.86f), y(0.94f))
        path.lineTo(x(0.2f), y(0.94f))
        path.close()
        p.style = Paint.Style.FILL
        p.pathEffect = CornerPathEffect(s * 0.07f)
        p.shader = LinearGradient(
            x(0.2f), y(0.06f), x(0.86f), y(0.94f),
            0xFFFF4D7A.toInt(), 0xFFE01B50.toInt(), Shader.TileMode.CLAMP
        )
        c.drawPath(path, p)
        p.shader = null
        p.pathEffect = null
        path.reset()
        path.moveTo(x(0.62f), y(0.06f))
        path.lineTo(x(0.86f), y(0.3f))
        path.lineTo(x(0.62f), y(0.3f))
        path.close()
        p.color = 0xCCFFFFFF.toInt()
        c.drawPath(path, p)
        tp.textSize = s * 0.24f
        c.drawText("PDF", x(0.53f), y(0.7f), tp)
    }
}

// the purple / blue waves at the bottom of the home screen
class WaveView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = if (P.dark) 255 else 110

        path.reset()
        path.moveTo(0f, h * 0.32f)
        path.cubicTo(w * 0.22f, h * 0.02f, w * 0.5f, h * 0.78f, w, h * 0.22f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(
                withAlpha(0xFF7B2FF7.toInt(), a),
                withAlpha(0xFF3A1C9E.toInt(), a),
                withAlpha(0xFF0B3FA8.toInt(), a)
            ),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        c.drawPath(path, p)

        path.reset()
        path.moveTo(0f, h * 0.72f)
        path.cubicTo(w * 0.3f, h * 0.45f, w * 0.62f, h * 1.0f, w, h * 0.45f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.shader = LinearGradient(
            0f, 0f, w, 0f,
            withAlpha(0xFF1B3FD0.toInt(), a * 3 / 4), withAlpha(0xFF0E5BFF.toInt(), a * 3 / 4),
            Shader.TileMode.CLAMP
        )
        c.drawPath(path, p)
        p.shader = null
    }
}
