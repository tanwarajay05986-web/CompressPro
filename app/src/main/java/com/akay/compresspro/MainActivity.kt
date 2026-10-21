package com.akay.compresspro

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
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
import android.widget.ProgressBar
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

    internal val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    internal val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    internal var screen = "home"

    // where to go after the login page ("home" or "settings")
    internal var loginReturn = "home"

    private val cui = CompressUi(this)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        Prefs.setAsked(true)
        if (hasPerm()) startApp() else showPermission()
    }

    private val imgPicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) loadItems(uris, false)
    }

    private val pdfPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) loadItems(uris, true)
    }

    private val accountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        val name = r.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (r.resultCode == Activity.RESULT_OK && !name.isNullOrEmpty()) {
            Prefs.setEmail(name)
            Prefs.setLoginSeen(true)
            toast(tr("Signed in as") + " " + name)
            leaveLogin()
        } else {
            toast(tr("No Google account was chosen"))
        }
    }

    // ---------- small helpers ----------

    internal fun tr(s: String): String = T.t(s)

    internal fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, h, wt)

    // size values above 0 are dp, MATCH / WRAP are kept as they are
    internal fun lpm(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(if (w > 0) dp(w) else w, if (h > 0) dp(h) else h)
        p.setMargins(dp(l), dp(t), dp(r), dp(b))
        return p
    }

    internal fun rr(color: Int, rDp: Int, stroke: Int = 0, strokeDp: Int = 0): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(rDp).toFloat()
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke)
        return d
    }

    internal fun grad(colors: IntArray, o: GradientDrawable.Orientation, rDp: Int): GradientDrawable {
        val d = GradientDrawable(o, colors)
        d.cornerRadius = dp(rDp).toFloat()
        return d
    }

    // style: 0 normal, 1 medium, 2 bold
    internal fun tv(text: String, sp: Float, color: Int, style: Int = 0): TextView {
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

    // bold text with a left-to-right colour gradient
    internal fun gradText(text: String, sp: Float, c1: Int, c2: Int): TextView {
        val t = tv(text, sp, c1, 2)
        t.post {
            val w = t.paint.measureText(t.text.toString())
            t.paint.shader = LinearGradient(0f, 0f, w, 0f, c1, c2, Shader.TileMode.CLAMP)
            t.invalidate()
        }
        return t
    }

    // shows a screen. `bottom` is the colour of the strip behind the phone's navigation bar
    internal fun setScreen(content: View, bottom: Int) {
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

    // ---------- login ----------

    // opens the phone's Google account chooser
    @Suppress("DEPRECATION")
    internal fun pickGoogleAccount() {
        try {
            val i = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), null, null, null, null
            )
            accountLauncher.launch(i)
        } catch (e: Throwable) {
            toast(tr("Could not open account picker"))
        }
    }

    internal fun leaveLogin() {
        if (loginReturn == "settings") showSettings() else showHome()
    }

    private fun startApp() {
        if (!Prefs.loginSeen()) showLogin("home") else showHome()
    }

    // ---------- permission ----------

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
        val t = tv(tr("Permission needed"), 24f, P.text, 2)
        t.gravity = Gravity.CENTER
        c.addView(t, lpm(MATCH, WRAP, 0, 26, 0, 10))
        val s = tv(
            tr("CompressPro needs access to your photos to pick images and PDFs. Without this permission the app cannot work."),
            15f, P.sub
        )
        s.gravity = Gravity.CENTER
        c.addView(s, lp(MATCH, WRAP))

        val blocked = permBlocked()
        val b = tv(tr(if (blocked) "Open Settings" else "Allow access"), 18f, Color.WHITE, 2)
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

    // ---------- lifecycle ----------

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
                when (screen) {
                    "settings", "compress" -> showHome()
                    "login" -> {
                        Prefs.setLoginSeen(true)
                        leaveLogin()
                    }
                    else -> finish()
                }
            }
        })
        if (hasPerm()) {
            startApp()
        } else {
            showPermission()
            if (!Prefs.asked()) permLauncher.launch(permList())
        }
    }

    override fun onResume() {
        super.onResume()
        if (screen == "perm" && hasPerm()) {
            startApp()
        } else if (screen != "perm" && !hasPerm()) {
            showPermission()
        }
    }

    // ---------- picking files ----------

    private fun pickFiles(pdf: Boolean) {
        if (pdf) pdfPicker.launch(arrayOf("application/pdf")) else imgPicker.launch("image/*")
    }

    // reads the chosen files in the background, then opens the Compression Settings screen
    private fun loadItems(uris: List<Uri>, pdf: Boolean) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.setPadding(dp(24), dp(20), dp(24), dp(20))
        box.addView(ProgressBar(this), lp(dp(40), dp(40)))
        val t = tv(tr("Loading…"), 16f, Color.WHITE)
        t.setPadding(dp(16), 0, 0, 0)
        box.addView(t)
        val dlg = AlertDialog.Builder(this).setView(box).setCancelable(false).create()
        dlg.show()
        val app = applicationContext
        Thread {
            val list = ArrayList<Item>()
            for (u in uris.take(30)) {
                val item = Engine.load(app, u, pdf)
                if (item != null) list.add(item)
            }
            runOnUiThread {
                try {
                    dlg.dismiss()
                } catch (e: Throwable) {
                }
                if (list.isEmpty()) toast(tr("Could not open the selected files")) else cui.show(pdf, list)
            }
        }.start()
    }

    // ---------- shared pieces ----------

    internal fun logoTile(sizeDp: Int): View {
        val f = FrameLayout(this)
        f.background = grad(
            intArrayOf(0xFF12C2FF.toInt(), 0xFF3B5BFF.toInt(), 0xFF8A3DFF.toInt()),
            GradientDrawable.Orientation.TL_BR, (sizeDp * 0.28f).toInt()
        )
        val ic = dp((sizeDp * 0.62f).toInt())
        f.addView(IconView(this, K.IMAGE, Color.WHITE), FrameLayout.LayoutParams(ic, ic, Gravity.CENTER))
        return f
    }

    internal fun brandRow(sp: Float): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.addView(tv("Compress", sp, P.text, 2))
        r.addView(gradText("Pro", sp, 0xFF00C8FF.toInt(), 0xFF3B7BFF.toInt()))
        return r
    }

    internal fun tagline(sp: Float): TextView =
        tv(tr("Smaller Size") + "  \u2022  " + tr("Same Quality"), sp, if (P.dark) 0xFF9DB8E8.toInt() else P.sub)

    internal fun navBar(sel: Int): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        val d = GradientDrawable()
        d.setColor(P.navBg)
        val r = dp(28).toFloat()
        d.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        d.setStroke(dp(1), P.cardEdge)
        bar.background = d
        bar.elevation = dp(6).toFloat()
        bar.setPadding(dp(12), dp(8), dp(12), dp(10))
        for (i in 0..1) {
            val on = i == sel
            val col = if (on) P.blue else P.navOff
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.addView(IconView(this, if (i == 0) K.HOME else K.GEAR, col), lp(dp(28), dp(28)))
            val t = tv(tr(if (i == 0) "Home" else "Settings"), 11f, col, 1)
            t.gravity = Gravity.CENTER
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

    // ---------- home ----------

    private fun imageTile(): View {
        val f = FrameLayout(this)
        f.background = rr(if (P.dark) 0x33FFFFFF else 0x99FFFFFF.toInt(), 22)
        val ic = if (P.dark) Color.WHITE else 0xFF3B8CFF.toInt()
        f.addView(IconView(this, K.IMAGE, ic), FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER))
        f.addView(badge(0xFF2F5BFF.toInt(), 0xFFB9D4FF.toInt()), badgeLp())
        return f
    }

    private fun pdfTile(): View {
        val f = FrameLayout(this)
        f.background = rr(if (P.dark) 0x33FFFFFF else 0x99FFFFFF.toInt(), 22)
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
        val dk = P.dark
        val tc = if (dk) Color.WHITE else 0xFF0F1B3D.toInt()
        val dc = if (dk) 0xDDFFFFFF.toInt() else 0xFF5F6F90.toInt()

        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        card.background = grad(colors, GradientDrawable.Orientation.TL_BR, 26)
        card.elevation = dp(if (dk) 8 else 5).toFloat()
        card.outlineSpotShadowColor = shadow
        card.outlineAmbientShadowColor = shadow
        card.addView(tile, lp(dp(78), dp(78)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), 0, dp(6), 0)
        col.addView(tv(title, 19f, tc, 2))
        col.addView(tv(sub, 15f, tc, 1))
        val d = tv(desc, 12f, dc)
        d.setPadding(0, dp(6), 0, 0)
        col.addView(d)
        card.addView(col, lp(0, WRAP, 1f))

        val chev = FrameLayout(this)
        val cd = GradientDrawable()
        cd.shape = GradientDrawable.OVAL
        cd.setColor(if (dk) 0x553A1AA0 else 0x55B7A9F5)
        chev.background = cd
        chev.addView(
            IconView(this, K.CHEV_R, if (dk) Color.WHITE else 0xFF2D3A7A.toInt()),
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
        )
        card.addView(chev, lp(dp(42), dp(42)))
        card.setOnClickListener { onClick() }
        return card
    }

    internal fun showHome() {
        cui.leave()
        screen = "home"
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(20), dp(16), dp(20), dp(16))

        // top row: logo, name, crown
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

        // headline and picture
        val hero = LinearLayout(this)
        hero.orientation = LinearLayout.HORIZONTAL
        hero.gravity = Gravity.CENTER_VERTICAL
        val hl = LinearLayout(this)
        hl.orientation = LinearLayout.VERTICAL
        hl.addView(tv(tr("Make Your Files"), 20f, P.text, 2))
        val line2 = LinearLayout(this)
        line2.orientation = LinearLayout.HORIZONTAL
        line2.addView(gradText(tr("Smaller"), 20f, 0xFF00C8FF.toInt(), 0xFF3B7BFF.toInt()))
        line2.addView(tv(" & ", 20f, P.text, 2))
        line2.addView(gradText(tr("Lighter"), 20f, 0xFF8A5BFF.toInt(), 0xFFC056FF.toInt()))
        hl.addView(line2)
        val d = tv(tr("Compress and resize your images and PDFs in just a few taps."), 13f, P.sub)
        d.setPadding(0, dp(10), 0, 0)
        hl.addView(d)
        hero.addView(hl, lp(0, WRAP, 1f))
        hero.addView(HeroArt(this), lp(dp(124), dp(116)))
        c.addView(hero, lpm(MATCH, WRAP, 0, 20, 0, 0))

        // the two big cards (saturated in Dark, pastel in White)
        val imgCols = if (P.dark) intArrayOf(0xFF12C2FF.toInt(), 0xFF2F6BFF.toInt(), 0xFF7A2BFF.toInt())
        else intArrayOf(0xFFBDE7FF.toInt(), 0xFFCDD3FF.toInt(), 0xFFE2CFFF.toInt())
        val pdfCols = if (P.dark) intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt())
        else intArrayOf(0xFFFFD8B2.toInt(), 0xFFFFC7D4.toInt(), 0xFFEACCFF.toInt())
        c.addView(
            bigCard(
                imgCols, imageTile(), tr("Image"), tr("Compress / Resize"),
                tr("Reduce image size and adjust dimensions without losing quality."),
                if (P.dark) 0xFF2F6BFF.toInt() else 0xFF9DB7F0.toInt()
            ) { pickFiles(false) },
            lpm(MATCH, WRAP, 0, 20, 0, 0)
        )
        c.addView(
            bigCard(
                pdfCols, pdfTile(), tr("PDF"), tr("Compress / Resize"),
                tr("Make your PDF files smaller and fit your needs."),
                if (P.dark) 0xFFFF3D6E.toInt() else 0xFFF0A3B5.toInt()
            ) { pickFiles(true) },
            lpm(MATCH, WRAP, 0, 14, 0, 0)
        )

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(c)

        // the waves sit under the cards (not behind them), the bar sits on top of the waves
        val bottom = FrameLayout(this)
        bottom.addView(WaveView(this), FrameLayout.LayoutParams(MATCH, dp(140), Gravity.BOTTOM))
        bottom.addView(navBar(0), FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.addView(sv, lp(MATCH, 0, 1f))
        root.addView(bottom, lp(MATCH, WRAP))
        setScreen(root, P.navBg)
    }
}
