package com.akay.compresspro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View

// picture at the top right of the Compression Settings screen
class SettingsArt(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    private fun card(
        c: Canvas, l: Float, t: Float, r: Float, b: Float, rot: Float,
        c1: Int, c2: Int, glyph: Boolean
    ) {
        c.save()
        c.rotate(rot, (l + r) / 2f, (t + b) / 2f)
        val w = r - l
        val h = b - t
        rf.set(l, t, r, b)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(l, t, r, b, c1, c2, Shader.TileMode.CLAMP)
        c.drawRoundRect(rf, w * 0.14f, w * 0.14f, p)
        p.shader = null
        if (glyph) {
            p.color = 0x55FFFFFF
            path.reset()
            path.moveTo(l + w * 0.1f, t + h * 0.84f)
            path.lineTo(l + w * 0.38f, t + h * 0.5f)
            path.lineTo(l + w * 0.55f, t + h * 0.7f)
            path.lineTo(l + w * 0.7f, t + h * 0.46f)
            path.lineTo(l + w * 0.9f, t + h * 0.84f)
            path.close()
            c.drawPath(path, p)
            p.color = Color.WHITE
            c.drawCircle(l + w * 0.3f, t + h * 0.3f, w * 0.07f, p)
        }
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        card(c, 0f, h * 0.12f, w * 0.62f, h * 0.7f, -9f, 0xFF1A4FD8.toInt(), 0xFF3C7BFF.toInt(), false)
        card(c, w * 0.18f, h * 0.2f, w * 0.8f, h * 0.8f, 7f, 0xFF22C0FF.toInt(), 0xFF2F6BFF.toInt(), true)

        rf.set(w * 0.68f, h * 0.5f, w * 0.98f, h * 0.88f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            rf.left, rf.top, rf.right, rf.bottom,
            0xFF8A4DFF.toInt(), 0xFF6130F0.toInt(), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rf, w * 0.07f, w * 0.07f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.color = Color.WHITE
        p.strokeWidth = w * 0.028f
        p.strokeCap = Paint.Cap.ROUND
        val cx = rf.centerX()
        val cy = rf.centerY()
        val bs = rf.width()
        for (sx in intArrayOf(-1, 1)) {
            for (sy in intArrayOf(-1, 1)) {
                val tx = cx + sx * bs * 0.1f
                val ty = cy + sy * bs * 0.1f
                c.drawLine(cx + sx * bs * 0.32f, cy + sy * bs * 0.32f, tx, ty, p)
                c.drawLine(tx, ty, tx + sx * bs * 0.18f, ty, p)
                c.drawLine(tx, ty, tx, ty + sy * bs * 0.18f, p)
            }
        }
    }
}

// quality slider: gradient fill and a white round thumb
class QSeek(ctx: Context) : View(ctx) {
    var min = 10
    var max = 100
    var value = 60
        set(v) {
            field = v.coerceIn(min, max)
            invalidate()
        }
    var onChange: ((Int) -> Unit)? = null
    var onDone: (() -> Unit)? = null

    private val den = ctx.resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    private fun pad(): Float = 14f * den

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val l = pad()
        val r = w - pad()
        val cy = h / 2f
        val th = 8f * den
        val f = (value - min).toFloat() / (max - min)
        val tx = l + (r - l) * f

        p.style = Paint.Style.FILL
        p.shader = null
        p.color = P.chip
        rf.set(l, cy - th / 2f, r, cy + th / 2f)
        c.drawRoundRect(rf, th / 2f, th / 2f, p)

        p.shader = LinearGradient(
            l, 0f, r, 0f,
            intArrayOf(0xFF00C8FF.toInt(), 0xFF2F6BFF.toInt(), 0xFF8A3DFF.toInt()),
            null, Shader.TileMode.CLAMP
        )
        rf.set(l, cy - th / 2f, maxOf(tx, l + th), cy + th / 2f)
        c.drawRoundRect(rf, th / 2f, th / 2f, p)
        p.shader = null

        p.color = 0x33000000
        c.drawCircle(tx, cy + 1.5f * den, 12f * den, p)
        p.color = Color.WHITE
        c.drawCircle(tx, cy, 11f * den, p)
    }

    private fun setFrom(x: Float) {
        val l = pad()
        val r = width - pad()
        val f = ((x - l) / (r - l)).coerceIn(0f, 1f)
        val v = Math.round(min + f * (max - min))
        if (v != value) {
            value = v
            onChange?.invoke(v)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                setFrom(e.x)
            }
            MotionEvent.ACTION_MOVE -> setFrom(e.x)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onDone?.invoke()
            }
        }
        return true
    }
}

// on / off switch (blue when on)
class SwitchView(ctx: Context) : View(ctx) {
    var on = true
        set(v) {
            field = v
            invalidate()
        }
    var onToggle: ((Boolean) -> Unit)? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    init {
        setOnClickListener {
            if (isEnabled) {
                on = !on
                onToggle?.invoke(on)
            }
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = if (on) 0xFF1E78FF.toInt() else 0xFF3A4A6B.toInt()
        rf.set(0f, 0f, w, h)
        c.drawRoundRect(rf, h / 2f, h / 2f, p)
        p.color = Color.WHITE
        val kx = if (on) w - h / 2f else h / 2f
        c.drawCircle(kx, h / 2f, h * 0.38f, p)
    }
}

// thin gradient progress bar
class HBar(ctx: Context) : View(ctx) {
    var frac = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = P.chip
        rf.set(0f, 0f, w, h)
        c.drawRoundRect(rf, h / 2f, h / 2f, p)
        p.shader = LinearGradient(
            0f, 0f, w, 0f, 0xFF00C8FF.toInt(), 0xFF2F6BFF.toInt(), Shader.TileMode.CLAMP
        )
        rf.set(0f, 0f, maxOf(w * frac, h), h)
        c.drawRoundRect(rf, h / 2f, h / 2f, p)
        p.shader = null
    }
}
