package com.akay.compresspro

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView

// set this to true after your other apps are on the Play Store
private const val MORE_LIVE = false

private class MoreApp(val name: String, val pkg: String, val kind: Int)

private val MORE_APPS = listOf(
    MoreApp("PDF Scanner", "com.akay.pdfscanner", 0),
    MoreApp("Video Player", "com.akay.videoplayer", 1),
    MoreApp("Block", "com.akay.block", 2)
)

// Hindi for the new texts of this file and of the result screen
object T2 {
    private val H: Map<String, String> = mapOf(
        "More Apps" to "और ऐप्स",
        "Discover more useful apps" to "और उपयोगी ऐप्स देखें",
        "Coming soon on Play Store" to "जल्द ही Play Store पर आएगा"
    )

    fun t(s: String): String = if (T.hi) (H[s] ?: s) else s
}

// the four small squares icon of the "More Apps" card
class GridIcon(ctx: Context, private val color: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.1f * s
        p.color = color
        val q = 0.38f * s
        val gap = 0.14f * s
        for (r in 0..1) {
            for (cc in 0..1) {
                val l = ox + 0.04f * s + cc * (q + gap)
                val t = oy + 0.04f * s + r * (q + gap)
                rf.set(l, t, l + q, t + q)
                c.drawRoundRect(rf, 0.1f * s, 0.1f * s, p)
            }
        }
    }
}

// the small logo of one of my other apps
class AppLogo(ctx: Context, private val kind: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    private fun sq(c: Canvas, ox: Float, oy: Float, s: Float, x: Float, y: Float, col: Int) {
        p.color = col
        rf.set(ox + x * s, oy + y * s, ox + (x + 0.26f) * s, oy + (y + 0.26f) * s)
        c.drawRoundRect(rf, 0.04f * s, 0.04f * s, p)
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        fun x(v: Float): Float = ox + v * s
        fun y(v: Float): Float = oy + v * s
        val r = s * 0.24f
        rf.set(ox, oy, ox + s, oy + s)
        p.style = Paint.Style.FILL
        p.shader = null

        if (kind == 0) {
            // PDF Scanner: blue square, white page, green scan line
            p.shader = LinearGradient(ox, oy, ox + s, oy + s, 0xFF29B6F6.toInt(), 0xFF0277BD.toInt(), Shader.TileMode.CLAMP)
            c.drawRoundRect(rf, r, r, p)
            p.shader = null
            path.reset()
            path.moveTo(x(0.28f), y(0.16f))
            path.lineTo(x(0.58f), y(0.16f))
            path.lineTo(x(0.76f), y(0.34f))
            path.lineTo(x(0.76f), y(0.84f))
            path.lineTo(x(0.28f), y(0.84f))
            path.close()
            p.color = Color.WHITE
            c.drawPath(path, p)
            path.reset()
            path.moveTo(x(0.58f), y(0.16f))
            path.lineTo(x(0.76f), y(0.34f))
            path.lineTo(x(0.58f), y(0.34f))
            path.close()
            p.color = 0xFFB3E5FC.toInt()
            c.drawPath(path, p)
            p.color = 0xFF90A4AE.toInt()
            rf.set(x(0.36f), y(0.42f), x(0.68f), y(0.46f))
            c.drawRect(rf, p)
            rf.set(x(0.36f), y(0.7f), x(0.6f), y(0.74f))
            c.drawRect(rf, p)
            p.color = 0xFF2ECC71.toInt()
            rf.set(x(0.2f), y(0.54f), x(0.84f), y(0.6f))
            c.drawRoundRect(rf, 0.03f * s, 0.03f * s, p)
        } else if (kind == 1) {
            // Video Player: blue circle with a white play triangle
            p.color = Color.WHITE
            c.drawRoundRect(rf, r, r, p)
            p.color = 0xFF2497F3.toInt()
            c.drawCircle(x(0.5f), y(0.5f), 0.38f * s, p)
            path.reset()
            path.moveTo(x(0.42f), y(0.3f))
            path.lineTo(x(0.7f), y(0.5f))
            path.lineTo(x(0.42f), y(0.7f))
            path.close()
            p.color = Color.WHITE
            c.drawPath(path, p)
        } else {
            // Block: four coloured squares on dark blue
            p.color = 0xFF2B3F8F.toInt()
            c.drawRoundRect(rf, r, r, p)
            sq(c, ox, oy, s, 0.22f, 0.22f, 0xFFE53935.toInt())
            sq(c, ox, oy, s, 0.52f, 0.22f, 0xFFFFC21A.toInt())
            sq(c, ox, oy, s, 0.22f, 0.52f, 0xFF2FBF3C.toInt())
            sq(c, ox, oy, s, 0.52f, 0.52f, 0xFF3C8CF0.toInt())
        }
    }
}

private fun MainActivity.openMore(app: MoreApp) {
    if (!MORE_LIVE) {
        toast(T2.t("Coming soon on Play Store"))
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app.pkg)))
    } catch (e: Throwable) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app.pkg))
            )
        } catch (e2: Throwable) {
            toast(tr("Could not open Play Store"))
        }
    }
}

// the "More Apps" sheet: only the name and the logo of every app
fun MainActivity.showMoreApps() {
    val dlg = Dialog(this)
    dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(20), dp(18), dp(20), dp(30))
    val bg = GradientDrawable()
    bg.setColor(P.card)
    val rad = dp(28).toFloat()
    bg.cornerRadii = floatArrayOf(rad, rad, rad, rad, 0f, 0f, 0f, 0f)
    c.background = bg

    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    head.addView(tv(T2.t("More Apps"), 20f, P.text, 2), lp(0, WRAP, 1f))
    val close: TextView = tv("\u2715", 18f, P.sub, 1)
    close.setPadding(dp(10), dp(4), dp(4), dp(4))
    close.setOnClickListener { dlg.dismiss() }
    head.addView(close)
    c.addView(head, lp(MATCH, WRAP))
    c.addView(tv(T2.t("Discover more useful apps"), 12.5f, P.sub), lpm(MATCH, WRAP, 0, 2, 0, 12))

    for (app in MORE_APPS) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = rr(P.field, 18, P.cardEdge, 1)
        row.setPadding(dp(12), dp(12), dp(14), dp(12))
        row.addView(AppLogo(this, app.kind), lp(dp(52), dp(52)))
        val name = tv(app.name, 16f, P.text, 2)
        name.setPadding(dp(14), 0, dp(6), 0)
        row.addView(name, lp(0, WRAP, 1f))
        row.addView(IconView(this, K.CHEV_R, P.sub), lp(dp(18), dp(18)))
        row.setOnClickListener { openMore(app) }
        c.addView(row, lpm(MATCH, WRAP, 0, 8, 0, 0))
    }

    dlg.setContentView(c)
    val w = dlg.window
    if (w != null) {
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        w.setLayout(MATCH, WRAP)
        w.setGravity(Gravity.BOTTOM)
    }
    dlg.show()
}
