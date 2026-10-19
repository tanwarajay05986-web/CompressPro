package com.akay.compresspro

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private var screen = "home"

    private val PRIVACY_URL = ""

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        Prefs.setAsked(true)
        if (hasPerm()) showHome() else showPermission()
    }

    private fun tr(s: String): String = T.t(s)

    private fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, h, wt)

    private fun lpm(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.setMargins(dp(l), dp(t), dp(r), dp(b))
        return p
    }

    private fun rr(color: Int, rDp: Int, stroke: Int = 0, strokeDp: Int = 0): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(rDp).toFloat()
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke)
        return d
    }

    private fun grad(colors: IntArray, o: GradientDrawable.Orientation, rDp: Int): GradientDrawable {
        val d = GradientDrawable(o, colors)
        d.cornerRadius = dp(rDp).toFloat()
        return d
    }

    private fun tv(text: String, sp: Float, color: Int, style: Int = 0): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = sp
        t.setTextColor(color)
        t.typeface = when (style) {
            2 -> Typeface.DEFAULT_BOLD
            1 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
        return t
    }

    private fun gradText(text: String, sp: Float, c1: Int, c2: Int): TextView {
        val t = tv(text, sp, c1, 2)
        t.post {
            val w = t.paint.measureText(t.text.toString())
            t.paint.shader = LinearGradient(0f, 0f, w, 0f, c1, c2, Shader.TileMode.CLAMP)
            t.invalidate()
        }
        return t
    }

    private fun setScreen(content: View, bottom: Int) {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setBackgroundColor(P.bg)
        val tp = View(this)
        val bp = View(this)
        bp.setBackgroundColor(bottom)
        wrap.addView(tp, lp(MATCH, 0))
        wrap.addView(content, lp(MATCH, 0, 1f))
        wrap.addView(bp, lp(MATCH, 0))
        ViewCompat.setOnApplyWindowInsetsListener(wrap) { _, ins ->
            val sb = ins.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = ins.getInsets(WindowInsetsCompat.Type.ime())
            (tp.layoutParams as LinearLayout.LayoutParams).height = sb.top
            (bp.layoutParams as LinearLayout.LayoutParams).height = maxOf(sb.bottom, ime.bottom)
            tp.requestLayout()
            bp.requestLayout()
            WindowInsetsCompat.CONSUMED
        }
        setContentView(wrap)
        val c = WindowInsetsControllerCompat(window, wrap)
        c.isAppearanceLightStatusBars = !P.dark
        c.isAppearanceLightNavigationBars = !P.dark
        ViewCompat.requestApplyInsets(wrap)
    }

    private fun permList(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun granted(p: String): Boolean =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun hasPerm(): Boolean = when {
        Build.VERSION.SDK_INT >= 34 ->
            granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> granted(Manifest.permission.READ_MEDIA_IMAGES)
        else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun permBlocked(): Boolean =
        Prefs.asked() && !shouldShowRequestPermissionRationale(permList()[0])

    private fun showPermission() {
        screen = "perm"
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        c.setPadding(dp(32), dp(32), dp(32), dp(32))

        c.addView(logoTile(96), lp(dp(96), dp(96)))
        val t = tv(tr("Permission needed"), 20f, P.text, 2)
        t.gravity = Gravity.CENTER
        c.addView(t, lpm(MATCH, WRAP, 0, 26, 0, 10))
        val s = tv(
            tr("CompressPro needs access to your photos to pick images and PDFs. Without this permission the app cannot work."),
            13f, P.sub
        )
        s.gravity = Gravity.CENTER
        c.addView(s, lp(MATCH, WRAP))

        val blocked = permBlocked()
        val b = tv(tr(if (blocked) "Open Settings" else "Allow access"), 16f, Color.WHITE, 2)
        b.gravity = Gravity.CENTER
        b.background = grad(
            intArrayOf(0xFF12C2FF.toInt(), 0xFF3B5BFF.toInt(), 0xFF8A3DFF.toInt()),
            GradientDrawable.Orientation.LEFT_RIGHT, 30
        )
        b.setOnClickListener {
            if (blocked) {
                try {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                } catch (e: Throwable) {
                }
            } else {
                permLauncher.launch(permList())
            }
        }
        c.addView(b, lpm(MATCH, 58, 0, 34, 0, 0))
        setScreen(c, P.bg)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            window.isNavigationBarContrastEnforced = false
        } catch (e: Throwable) {
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (screen == "settings") showHome() else finish()
            }
        })
        if (hasPerm()) {
            showHome()
        } else {
            showPermission()
            if (!Prefs.asked()) permLauncher.launch(permList())
        }
    }

    override fun onResume() {
        super.onResume()
        if (screen == "perm" && hasPerm()) {
            showHome()
        } else if (screen != "perm" && !hasPerm()) {
            showPermission()
        }
    }

    private fun logoTile(sizeDp: Int): View {
        val f = FrameLayout(this)
        f.background = grad(
            intArrayOf(0xFF12C2FF.toInt(), 0xFF3B5BFF.toInt(), 0xFF8A3DFF.toInt()),
            GradientDrawable.Orientation.TL_BR, (sizeDp * 0.28f).toInt()
        )
        val ic = dp((sizeDp * 0.62f).toInt())
        f.addView(IconView(this, K.IMAGE, Color.WHITE), FrameLayout.LayoutParams(ic, ic, Gravity.CENTER))
        return f
    }

    private fun brandRow(sp: Float): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.addView(tv("Compress", sp, P.text, 2))
        r.addView(gradText("Pro", sp, 0xFF00C8FF.toInt(), 0xFF3B7BFF.toInt()))
        return r
    }

    private fun tagline(sp: Float): TextView =
        tv(tr("Smaller Size") + "  \u2022  " + tr("Same Quality"), sp, if (P.dark) 0xFF9DB8E8.toInt() else P.sub)

    private fun navBar(sel: Int): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        val d = GradientDrawable()
        d.setColor(P.navBg)
        val r = dp(28).toFloat()
        d.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        d.setStroke(dp(1), P.cardEdge)
        bar.background = d
        bar.setPadding(dp(12), dp(8), dp(12), dp(10))
        for (i in 0..1) {
            val on = i == sel
            val col = if (on) P.blue else P.navOff
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.addView(IconView(this, if (i == 0) K.HOME else K.GEAR, col), lp(dp(24), dp(24)))
            val t = tv(tr(if (i == 0) "Home" else "Settings"), 11f, col, 1)
            t.gravity = Gravity.CENTER_HORIZONTAL
            t.setPadding(0, dp(2), 0, dp(6))
            item.addView(t, lp(WRAP, WRAP))
            val line = View(this)
            if (on) line.background = rr(P.blue, 2)
            item.addView(line, lp(dp(66), dp(3)))
            item.setOnClickListener {
                if (i == 0 && screen != "home") showHome()
                if (i == 1 && screen != "settings") showSettings()
            }
            bar.addView(item, lp(0, WRAP, 1f))
        }
        return bar
    }

    private fun imageTile(): View {
        val f = FrameLayout(this)
        f.background = rr(0x33FFFFFF.toInt(), 22)
        f.addView(IconView(this, K.IMAGE, Color.WHITE), FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER))
        f.addView(badge(0xFF2F5BFF.toInt(), 0xFFB9D4FF.toInt()), badgeLp())
        return f
    }

    private fun pdfTile(): View {
        val f = FrameLayout(this)
        f.background = rr(0x33FFFFFF.toInt(), 22)
        f.addView(PdfGlyph(this), FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER))
        f.addView(badge(0xFFE0245A.toInt(), 0xFFFFB3C6.toInt()), badgeLp())
        return f
    }

    private fun badge(fill: Int, edge: Int): View {
        val b = FrameLayout(this)
        val bd = GradientDrawable()
        bd.shape = GradientDrawable.OVAL
        bd.setColor(fill)
        bd.setStroke(dp(2), edge)
        b.background = bd
        b.addView(IconView(this, K.RESIZE, Color.WHITE), FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
        return b
    }

    private fun badgeLp(): FrameLayout.LayoutParams {
        val l = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.END)
        l.rightMargin = dp(2)
        l.bottomMargin = dp(2)
        return l
    }

    private fun bigCard(
        colors: IntArray, tile: View, title: String, sub: String, desc: String,
        shadow: Int, onClick: () -> Unit
    ): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(dp(16), dp(18), dp(16), dp(18))
        card.background = grad(colors, GradientDrawable.Orientation.TL_BR, 26)
        card.elevation = dp(8).toFloat()
        card.outlineSpotShadowColor = shadow
        card.outlineAmbientShadowColor = shadow
        card.addView(tile, lp(dp(78), dp(78)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), 0, dp(6), 0)
        col.addView(tv(title, 19f, Color.WHITE, 2))
        col.addView(tv(sub, 15f, Color.WHITE, 1))
        val d = tv(desc, 12.5f, 0xDDFFFFFF.toInt())
        d.setPadding(0, dp(8), 0, 0)
        col.addView(d)
        card.addView(col, lp(0, WRAP, 1f))

        val chev = FrameLayout(this)
        val cd = GradientDrawable()
        cd.shape = GradientDrawable.OVAL
        cd.setColor(0x553A1AA0.toInt())
        chev.background = cd
        chev.addView(IconView(this, K.CHEV_R, Color.WHITE), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        card.addView(chev, lp(dp(42), dp(42)))
        card.setOnClickListener { onClick() }
        return card
    }

    private fun showHome() {
        screen = "home"
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(20), dp(16), dp(20), dp(20))

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(logoTile(58), lp(dp(58), dp(58)))
        val ht = LinearLayout(this)
        ht.orientation = LinearLayout.VERTICAL
        ht.setPadding(dp(14), 0, dp(8), 0)
        ht.addView(brandRow(20f))
        ht.addView(tagline(11f))
        head.addView(ht, lp(0, WRAP, 1f))
        val crown = IconView(this, K.CROWN, P.gold)
        crown.setOnClickListener { toast(tr("CompressPro is 100% free. No subscription!")) }
        head.addView(crown, lp(dp(34), dp(34)))
        c.addView(head, lp(MATCH, WRAP))

        val hero = LinearLayout(this)
        hero.orientation = LinearLayout.HORIZONTAL
        hero.gravity = Gravity.CENTER_VERTICAL
        val hl = LinearLayout(this)
        hl.orientation = LinearLayout.VERTICAL
        hl.addView(tv(tr("Make Your Files"), 20f, P.text, 2))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(gradText(tr("Smaller"), 20f, 0xFF00C8FF.toInt(), 0xFF3B7BFF.toInt()))
        row.addView(tv(" & ", 20f, P.text, 2))
        row.addView(gradText(tr("Lighter"), 20f, 0xFF8A5BFF.toInt(), 0xFFC056FF.toInt()))
        hl.addView(row)
        val d = tv(tr("Compress and resize your images and PDFs in just a few taps."), 12f, P.sub)
        d.setPadding(0, dp(10), 0, 0)
        hl.addView(d)
        hero.addView(hl, lp(0, WRAP, 1f))
        hero.addView(HeroArt(this), lp(dp(124), dp(116)))
        c.addView(hero, lpm(MATCH, WRAP, 0, 24, 0, 0))

        val soon = { toast(tr("Coming in the next stage")) }
        c.addView(
            bigCard(
                intArrayOf(0xFF12C2FF.toInt(), 0xFF2F6BFF.toInt(), 0xFF7A2BFF.toInt()), imageTile(),
                tr("Image"), tr("Compress / Resize"),
                tr("Reduce image size and adjust dimensions without losing quality."),
                0xFF2F6BFF.toInt(), soon
            ),
            lpm(MATCH, WRAP, 0, 22, 0, 0)
        )
        c.addView(
            bigCard(
                intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()), pdfTile(),
                tr("PDF"), tr("Compress / Resize"),
                tr("Make your PDF files smaller and fit your needs."),
                0xFFFF3D6E.toInt(), soon
            ),
            lpm(MATCH, WRAP, 0, 14, 0, 0)
        )

        val frame = FrameLayout(this)
        frame.addView(WaveView(this), FrameLayout.LayoutParams(MATCH, dp(130), Gravity.BOTTOM))
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(c)
        frame.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.addView(frame, lp(MATCH, 0, 1f))
        root.addView(navBar(0), lp(MATCH, WRAP))
        setScreen(root, P.navBg)
    }

    private fun cardBox(): LinearLayout {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.VERTICAL
        b.background = rr(P.card, 22, P.cardEdge, 1)
        b.setPadding(dp(16), dp(4), dp(16), dp(4))
        return b
    }

    private fun row(
        icon: Int, title: String, sub: String?, value: String?, last: Boolean, onClick: () -> Unit
    ): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(0, dp(14), 0, dp(14))
        r.addView(IconView(this, icon, P.iconSoft), lp(dp(34), dp(34)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), 0, dp(6), 0)
        col.addView(tv(title, 15f, P.text, 1))
        if (sub != null) col.addView(tv(sub, 12f, P.sub))
        r.addView(col, lp(0, WRAP, 1f))
        if (value != null) {
            val v = tv(value, 14f, P.sub)
            v.setPadding(0, 0, dp(8), 0)
            r.addView(v)
        }
        r.addView(IconView(this, K.CHEV_R, P.sub), lp(dp(18), dp(18)))
        r.setOnClickListener { onClick() }
        box.addView(r)
        if (!last) {
            val dv = View(this)
            dv.setBackgroundColor(P.divider)
            box.addView(dv, lpm(MATCH, 1, 54, 0, 0, 0))
        }
        return box
    }

    private fun choose(title: String, items: Array<String>, sel: Int, onPick: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items, sel) { d, w ->
                d.dismiss()
                onPick(w)
            }
            .setNegativeButton(tr("Cancel"), null)
            .show()
    }

    private fun showSettings() {
        screen = "settings"
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(14), dp(18), dp(24))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val back = IconView(this, K.ARROW_L, P.text)
        back.setOnClickListener { showHome() }
        top.addView(back, lp(dp(32), dp(32)))
        val title = tv(tr("Settings"), 22f, P.text, 2)
        title.setPadding(dp(18), 0, 0, 0)
        top.addView(title)
        c.addView(top, lp(MATCH, WRAP))

        val app = LinearLayout(this)
        app.orientation = LinearLayout.HORIZONTAL
        app.gravity = Gravity.CENTER_VERTICAL
        app.background = rr(P.card, 22, P.cardEdge, 1)
        app.setPadding(dp(16), dp(16), dp(16), dp(16))
        app.addView(logoTile(62), lp(dp(62), dp(62)))
        val at = LinearLayout(this)
        at.orientation = LinearLayout.VERTICAL
        at.setPadding(dp(14), 0, 0, 0)
        at.addView(brandRow(18f))
        at.addView(tagline(11f))
        app.addView(at, lp(0, WRAP, 1f))
        c.addView(app, lpm(MATCH, WRAP, 0, 18, 0, 0))

        val themeName = tr(if (P.dark) "Dark" else "Light")
        val langName = if (T.hi) "हिन्दी" else "English"
        val qNames = arrayOf(tr("Low"), tr("Medium"), tr("High"))
        val sNames = arrayOf(tr("Internal"), tr("Downloads"))

        val list = cardBox()
        list.addView(row(K.PALETTE, tr("App Theme"), tr("Choose your preferred theme"), themeName, false) {
            choose(tr("App Theme"), arrayOf(tr("Dark"), tr("Light")), if (P.dark) 0 else 1) { w ->
                Prefs.setDark(w == 0)
                showSettings()
            }
        })
        list.addView(row(K.GLOBE, tr("Language"), tr("Select app language"), langName, false) {
            choose(tr("Language"), arrayOf("English", "हिन्दी"), Prefs.lang()) { w ->
                Prefs.setLang(w)
                showSettings()
            }
        })
        list.addView(row(K.SHIELD, tr("Output Quality"), tr("Set default quality f
