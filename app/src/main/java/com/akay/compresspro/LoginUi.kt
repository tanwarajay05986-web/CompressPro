package com.akay.compresspro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

// two photos (5.2 MB -> 1.1 MB). It always fits inside the space it gets.
class LoginArt(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    init {
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = Paint.Align.CENTER
        tp.color = Color.WHITE
    }

    private fun photo(c: Canvas, l: Float, t: Float, r: Float, b: Float, rot: Float) {
        c.save()
        c.rotate(rot, (l + r) / 2f, (t + b) / 2f)
        val w = r - l
        rf.set(l, t, r, b)
        p.style = Paint.Style.FILL
        p.color = if (P.dark) 0x33FFFFFF else 0xE6FFFFFF.toInt()
        c.drawRoundRect(rf, w * 0.1f, w * 0.1f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.02f
        p.color = if (P.dark) 0x55FFFFFF else 0xFFD6E2FA.toInt()
        c.drawRoundRect(rf, w * 0.1f, w * 0.1f, p)
        rf.inset(w * 0.07f, w * 0.07f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            rf.left, rf.top, rf.right, rf.bottom,
            0xFFB9E0FF.toInt(), 0xFF6FA8FF.toInt(), Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rf, w * 0.06f, w * 0.06f, p)
        p.shader = null
        val pl = rf.left
        val pt = rf.top
        val pw = rf.width()
        val ph = rf.height()
        p.color = 0xFF3B7BFF.toInt()
        path.reset()
        path.moveTo(pl + pw * 0.04f, pt + ph * 0.96f)
        path.lineTo(pl + pw * 0.34f, pt + ph * 0.5f)
        path.lineTo(pl + pw * 0.52f, pt + ph * 0.76f)
        path.lineTo(pl + pw * 0.7f, pt + ph * 0.46f)
        path.lineTo(pl + pw * 0.96f, pt + ph * 0.96f)
        path.close()
        c.drawPath(path, p)
        p.color = 0xFF2F6BFF.toInt()
        c.drawCircle(pl + pw * 0.24f, pt + ph * 0.24f, pw * 0.07f, p)
        c.restore()
    }

    private fun pill(c: Canvas, cx: Float, cy: Float, text: String, col: Int, size: Float) {
        tp.textSize = size
        val tw = tp.measureText(text)
        rf.set(cx - tw / 2f - size * 0.8f, cy - size * 0.95f, cx + tw / 2f + size * 0.8f, cy + size * 0.95f)
        p.style = Paint.Style.FILL
        p.color = col
        c.drawRoundRect(rf, size, size, p)
        c.drawText(text, cx, cy + size * 0.36f, tp)
    }

    private fun sparkle(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        p.style = Paint.Style.FILL
        p.color = col
        path.reset()
        path.moveTo(cx, cy - r)
        path.quadTo(cx, cy, cx + r, cy)
        path.quadTo(cx, cy, cx, cy + r)
        path.quadTo(cx, cy, cx - r, cy)
        path.quadTo(cx, cy, cx, cy - r)
        path.close()
        c.drawPath(path, p)
    }

    override fun onDraw(c: Canvas) {
        val aw = width.toFloat()
        val ah = height.toFloat()
        val aspect = 1.75f
        val w = minOf(aw, ah * aspect)
        val h = w / aspect
        c.save()
        c.translate((aw - w) / 2f, (ah - h) / 2f)

        rf.set(w * 0.04f, h * 0.2f, w * 0.96f, h * 0.98f)
        p.style = Paint.Style.FILL
        p.color = if (P.dark) 0x14FFFFFF else 0xFFEAEFFC.toInt()
        c.drawOval(rf, p)

        photo(c, w * 0.1f, h * 0.1f, w * 0.46f, h * 0.76f, -8f)
        photo(c, w * 0.58f, h * 0.3f, w * 0.84f, h * 0.76f, 6f)

        // purple arrow between the two photos
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.03f
        p.strokeCap = Paint.Cap.ROUND
        p.color = 0xFF8A5BFF.toInt()
        path.reset()
        path.moveTo(w * 0.47f, h * 0.55f)
        path.quadTo(w * 0.52f, h * 0.3f, w * 0.6f, h * 0.38f)
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        path.reset()
        path.moveTo(w * 0.62f, h * 0.3f)
        path.lineTo(w * 0.64f, h * 0.46f)
        path.lineTo(w * 0.52f, h * 0.4f)
        path.close()
        c.drawPath(path, p)

        pill(c, w * 0.3f, h * 0.74f, "5.2 MB", 0xFF8A93B5.toInt(), w * 0.04f)
        pill(c, w * 0.73f, h * 0.8f, "1.1 MB", 0xFF2F7DFF.toInt(), w * 0.04f)

        sparkle(c, w * 0.88f, h * 0.18f, w * 0.035f, 0xFF2FA8FF.toInt())
        sparkle(c, w * 0.94f, h * 0.34f, w * 0.026f, 0xFF3B7BFF.toInt())
        sparkle(c, w * 0.84f, h * 0.42f, w * 0.016f, 0xFF7C9CFF.toInt())
        c.restore()
    }
}

// the Google "G"
class GLogo(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        val sw = s * 0.2f
        rf.set(ox + sw / 2f, oy + sw / 2f, ox + s - sw / 2f, oy + s - sw / 2f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.strokeCap = Paint.Cap.BUTT
        p.color = 0xFFEA4335.toInt()
        c.drawArc(rf, 215f, 105f, false, p)
        p.color = 0xFFFBBC05.toInt()
        c.drawArc(rf, 135f, 80f, false, p)
        p.color = 0xFF34A853.toInt()
        c.drawArc(rf, 45f, 90f, false, p)
        p.color = 0xFF4285F4.toInt()
        c.drawArc(rf, -5f, 50f, false, p)
        p.style = Paint.Style.FILL
        val cy = oy + s / 2f
        c.drawRect(ox + s * 0.5f, cy - sw / 2f, ox + s * 0.97f, cy + sw / 2f, p)
    }
}

private fun MainActivity.feature(icon: Int, tint: Int, softBg: Int, full: String, l1: String, l2: String): View {
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.gravity = Gravity.CENTER_HORIZONTAL
    val tile = FrameLayout(this)
    tile.background = rr(if (P.dark) withAlpha(tint, 40) else softBg, 18)
    tile.addView(IconView(this, icon, tint), FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
    col.addView(tile, lp(dp(52), dp(52)))
    val text = if (T.hi) tr(full) else l1 + "\n" + l2
    val t = tv(text, 12f, P.sub)
    t.gravity = Gravity.CENTER
    col.addView(t, lpm(MATCH, WRAP, 0, 6, 0, 0))
    return col
}

// the login page: Continue with Google or Skip. Everything fits on one screen (no scrolling).
fun MainActivity.showLogin(from: String) {
    loginReturn = from
    screen = "login"

    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.gravity = Gravity.CENTER_HORIZONTAL
    // the bottom space lifts the buttons a little higher
    c.setPadding(dp(24), dp(10), dp(24), dp(56))

    // logo tile with the download badge
    val logo = FrameLayout(this)
    logo.background = grad(P.grad3, GradientDrawable.Orientation.TL_BR, 24)
    logo.addView(IconView(this, K.IMAGE, Color.WHITE), FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER))
    val badge = FrameLayout(this)
    val bd = GradientDrawable()
    bd.shape = GradientDrawable.OVAL
    bd.setColor(Color.WHITE)
    badge.background = bd
    badge.addView(IconView(this, K.DOWNLOAD, 0xFF3B5BFF.toInt()), FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER))
    val bp = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.END)
    bp.setMargins(0, 0, dp(4), dp(4))
    logo.addView(badge, bp)
    c.addView(logo, lp(dp(76), dp(76)))

    c.addView(brandRow(28f), lpm(WRAP, WRAP, 0, 8, 0, 0))
    c.addView(tagline(13f), lpm(WRAP, WRAP, 0, 2, 0, 0))

    // the picture takes whatever space is left
    c.addView(LoginArt(this), lp(MATCH, 0, 1f))

    val h1 = tv(tr("Compress Your Images"), 21f, P.text, 2)
    h1.gravity = Gravity.CENTER
    c.addView(h1, lp(WRAP, WRAP))
    val h2 = LinearLayout(this)
    h2.orientation = LinearLayout.HORIZONTAL
    h2.addView(tv(tr("& PDFs") + " ", 21f, P.text, 2))
    h2.addView(gradText(tr("Effortlessly"), 21f, 0xFF2F7DFF.toInt(), 0xFF8A3DFF.toInt()))
    c.addView(h2, lp(WRAP, WRAP))
    val sub = tv(tr("Save space, share faster, and keep your files in perfect quality."), 13.5f, P.sub)
    sub.gravity = Gravity.CENTER
    c.addView(sub, lpm(MATCH, WRAP, 12, 8, 12, 0))

    // four small features
    val feats = LinearLayout(this)
    feats.orientation = LinearLayout.HORIZONTAL
    feats.addView(
        feature(K.BOLT, 0xFF3B7BFF.toInt(), 0xFFE8EFFF.toInt(), "Fast Compression", "Fast", "Compression"),
        lp(0, WRAP, 1f)
    )
    feats.addView(
        feature(K.SHIELD_CHECK, 0xFF22C26E.toInt(), 0xFFDDF6EA.toInt(), "High Quality Output", "High Quality", "Output"),
        lp(0, WRAP, 1f)
    )
    feats.addView(
        feature(K.CLOUD, 0xFF7C4DFF.toInt(), 0xFFEADFFF.toInt(), "Easy to Share", "Easy to", "Share"),
        lp(0, WRAP, 1f)
    )
    feats.addView(
        feature(K.LOCK, 0xFFFFA11A.toInt(), 0xFFFFEBD9.toInt(), "Secure & Private", "Secure", "& Private"),
        lp(0, WRAP, 1f)
    )
    c.addView(feats, lpm(MATCH, WRAP, 0, 14, 0, 0))

    // Continue with Google
    val g = LinearLayout(this)
    g.orientation = LinearLayout.HORIZONTAL
    g.gravity = Gravity.CENTER_VERTICAL
    g.background = rr(P.card, 30, P.cardEdge, 1)
    g.elevation = dp(3).toFloat()
    g.setPadding(dp(22), 0, dp(22), 0)
    g.addView(GLogo(this), lp(dp(28), dp(28)))
    val gt = tv(tr("Continue with Google"), 17f, P.text, 2)
    gt.gravity = Gravity.CENTER
    g.addView(gt, lp(0, WRAP, 1f))
    g.addView(Icon2View(this, K2.ARROW_THIN, P.text), lp(dp(26), dp(26)))
    g.setOnClickListener { pickGoogleAccount() }
    c.addView(g, lpm(MATCH, 54, 0, 16, 0, 0))

    // Skip
    val s = LinearLayout(this)
    s.orientation = LinearLayout.HORIZONTAL
    s.gravity = Gravity.CENTER_VERTICAL
    s.background = rr(Color.TRANSPARENT, 26, if (P.dark) 0xFF3A4A8A.toInt() else 0xFFC9C6F5.toInt(), 1)
    s.setPadding(dp(22), 0, dp(22), 0)
    s.addView(View(this), lp(dp(26), dp(26)))
    val st = tv(tr("Skip"), 17f, P.sub, 1)
    st.gravity = Gravity.CENTER
    s.addView(st, lp(0, WRAP, 1f))
    s.addView(Icon2View(this, K2.ARROW_THIN, P.sub), lp(dp(26), dp(26)))
    s.setOnClickListener {
        Prefs.setLoginSeen(true)
        leaveLogin()
    }
    c.addView(s, lpm(MATCH, 50, 0, 10, 0, 0))

    val note = tv(tr("You can always sign in later from settings."), 12f, P.sub)
    note.gravity = Gravity.CENTER
    c.addView(note, lpm(MATCH, WRAP, 0, 10, 0, 0))

    setScreen(c, P.bg)
}
