package com.akay.compresspro

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.view.View
import android.view.animation.OvershootInterpolator

// the circle at the top of the result screen: a progress ring while compressing,
// then a green tick with sparkles when it is done
class StatusView(ctx: Context) : View(ctx) {
    var progress = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    private var done = false
    private var pop = 0f
    private var anim: ValueAnimator? = null
    private val den = ctx.resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()
    private val ovs = OvershootInterpolator(2.2f)

    init {
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = Paint.Align.CENTER
    }

    fun finish() {
        done = true
        anim?.cancel()
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 800
        a.addUpdateListener {
            pop = it.animatedValue as Float
            invalidate()
        }
        a.start()
        anim = a
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    private fun sparkle(c: Canvas, x: Float, y: Float, r: Float, col: Int, a: Int) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = withAlpha(col, a)
        path.reset()
        path.moveTo(x, y - r)
        path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r)
        path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        path.close()
        c.drawPath(path, p)
    }

    private fun swoosh(c: Canvas, cx: Float, cy: Float, r: Float, start: Float, sweep: Float, col: Int, sw: Float, a: Int) {
        p.style = Paint.Style.STROKE
        p.shader = null
        p.strokeWidth = sw
        p.strokeCap = Paint.Cap.ROUND
        p.color = withAlpha(col, a)
        rf.set(cx - r, cy - r, cx + r, cy + r)
        c.drawArc(rf, start, sweep, false, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val d = minOf(h * 0.84f, 110f * den)
        val r = d / 2f

        if (!done) {
            val sw = d * 0.1f
            val rr = r - sw / 2f
            p.style = Paint.Style.STROKE
            p.strokeWidth = sw
            p.strokeCap = Paint.Cap.ROUND
            p.shader = null
            p.color = if (P.dark) 0xFF14284F.toInt() else 0xFFE1EAFB.toInt()
            c.drawCircle(cx, cy, rr, p)
            val sweep = 360f * progress
            if (sweep > 1f) {
                p.shader = SweepGradient(
                    cx, cy,
                    intArrayOf(0xFF00C8FF.toInt(), 0xFF2F6BFF.toInt(), 0xFF8A3DFF.toInt(), 0xFF00C8FF.toInt()),
                    null
                )
                c.save()
                c.rotate(-90f, cx, cy)
                rf.set(cx - rr, cy - rr, cx + rr, cy + rr)
                c.drawArc(rf, 0f, sweep, false, p)
                c.restore()
                p.shader = null
            }
            tp.color = P.text
            tp.textSize = d * 0.27f
            c.drawText((progress * 100f).toInt().toString() + "%", cx, cy + tp.textSize * 0.35f, tp)
            return
        }

        val s = ovs.getInterpolation(pop.coerceIn(0f, 1f)).coerceAtLeast(0.01f)
        val al = (pop.coerceIn(0f, 1f) * 255f).toInt()

        // soft green glow
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, r * 1.7f,
            intArrayOf(withAlpha(0xFF2BE08A.toInt(), (90f * pop).toInt()), 0x00000000),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r * 1.7f, p)

        // green disc
        p.shader = LinearGradient(
            cx - r, cy - r, cx + r, cy + r,
            0xFF4BEFA0.toInt(), 0xFF12B981.toInt(), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r * s, p)
        p.shader = null

        // thin white ring and the tick
        p.style = Paint.Style.STROKE
        p.strokeWidth = d * 0.035f
        p.color = withAlpha(Color.WHITE, 200)
        c.drawCircle(cx, cy, r * s * 0.93f, p)
        p.strokeWidth = d * 0.1f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = Color.WHITE
        path.reset()
        path.moveTo(cx - d * 0.2f * s, cy + d * 0.01f * s)
        path.lineTo(cx - d * 0.05f * s, cy + d * 0.16f * s)
        path.lineTo(cx + d * 0.22f * s, cy - d * 0.14f * s)
        c.drawPath(path, p)

        // sparkles around
        sparkle(c, cx - 1.0f * d, cy - 0.42f * d, 0.07f * d, 0xFF22C7FF.toInt(), al)
        sparkle(c, cx + 0.5f * d, cy - 0.62f * d, 0.04f * d, 0xFF22C7FF.toInt(), al)
        sparkle(c, cx + 1.15f * d, cy + 0.05f * d, 0.11f * d, 0xFF3B7BFF.toInt(), al)
        sparkle(c, cx + 0.88f * d, cy + 0.58f * d, 0.05f * d, 0xFF8A5BFF.toInt(), al)
        sparkle(c, cx - 0.7f * d, cy + 0.62f * d, 0.035f * d, 0xFF22C7FF.toInt(), al)
        swoosh(c, cx - 1.0f * d, cy + 0.36f * d, 0.13f * d, 200f, 110f, 0xFFFFD84A.toInt(), d * 0.04f, al)
        swoosh(c, cx + 0.98f * d, cy - 0.5f * d, 0.13f * d, 300f, 100f, 0xFF19C6FF.toInt(), d * 0.04f, al)
    }
}

// ring that shows how much the file got smaller (green arc)
class RingStat(ctx: Context) : View(ctx) {
    var pct = 0f
        set(v) {
            field = v
            invalidate()
        }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val sw = s * 0.12f
        val rr = s / 2f - sw / 2f
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.strokeCap = Paint.Cap.ROUND
        p.shader = null
        p.color = if (P.dark) 0xFF14315A.toInt() else 0xFFE4EFF7.toInt()
        c.drawCircle(cx, cy, rr, p)
        val sweep = 360f * (pct / 100f).coerceIn(0f, 1f)
        if (sweep > 1f) {
            p.shader = SweepGradient(
                cx, cy,
                intArrayOf(0xFF19E39A.toInt(), 0xFF10C9A8.toInt(), 0xFF19E39A.toInt()),
                null
            )
            c.save()
            c.rotate(-90f, cx, cy)
            rf.set(cx - rr, cy - rr, cx + rr, cy + rr)
            c.drawArc(rf, 0f, sweep, false, p)
            c.restore()
            p.shader = null
        }
    }
}
