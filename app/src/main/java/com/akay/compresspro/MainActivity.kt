package com.akay.compresspro

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

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

    // ================= STAGE 2 / 3 : Compression state =================
    private val ACC1 = Color.parseColor("#3B82F6")
    private val ACC2 = Color.parseColor("#8B5CF6")
    private val SUBC = Color.parseColor("#94A3B8")
    private val CARD2 = Color.parseColor("#1E293B")
    private val GREEN = Color.parseColor("#22C55E")
    private val REDC = Color.parseColor("#EF4444")

    private var pickedUris: MutableList<Uri> = mutableListOf()
    private var pickedNames: MutableList<String> = mutableListOf()
    private var pickedSizes: MutableList<Long> = mutableListOf()
    private var selectedPickIndex: Int = 0
    private var isPdfMode: Boolean = false

    private var cQuality: Int = 60
    private var origW: Int = 1280
    private var origH: Int = 720
    private var lockAspect: Boolean = true
    private var outFormat: String = "JPG"
    private var cancelRequested = false

    private val estHandler = Handler(Looper.getMainLooper())
    private var estRunnable: Runnable? = null

    private lateinit var estSizeValue: TextView
    private lateinit var estSizePct: TextView
    private lateinit var estOrigText: TextView
    private lateinit var estBarFg: View
    private lateinit var estBarContainer: LinearLayout

    private lateinit var qualityPctChip: TextView
    private lateinit var qualitySeek: SeekBar
    private lateinit var qLowBtn: TextView
    private lateinit var qMedBtn: TextView
    private lateinit var qHighBtn: TextView

    private lateinit var dimPctChip: TextView
    private lateinit var heightEdit: EditText
    private lateinit var widthEdit: EditText
    private lateinit var p25: TextView
    private lateinit var p50: TextView
    private lateinit var p75: TextView
    private lateinit var pCustom: TextView

    private lateinit var fJpg: TextView
    private lateinit var fPng: TextView
    private lateinit var fWebp: TextView
    private lateinit var fOrig: TextView

    private lateinit var progPercentText: TextView
    private lateinit var progRing: RingProgressView
    private lateinit var progTimeText: TextView
    private lateinit var progSpeedText: TextView
    private lateinit var progOrigText: TextView
    private lateinit var progCompText: TextView
    private lateinit var progFileName: TextView
    private lateinit var progThumb: ImageView
    private lateinit var progBarFg: View
    private lateinit var progBarContainer: LinearLayout
    private lateinit var step1Dot: FrameLayout
    private lateinit var step2Dot: FrameLayout
    private lateinit var step3Dot: FrameLayout
    private lateinit var step2Status: TextView
    private lateinit var step3Status: TextView
    private lateinit var progBottomBtn: LinearLayout
    private lateinit var progBottomLabel: TextView
    private lateinit var progBottomSub: TextView

    private val imagePickLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                isPdfMode = false
                pickedUris = uris.toMutableList()
                loadPickedMeta()
                showCompressionSettings()
            }
        }

    private val pdfPickLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                isPdfMode = true
                pickedUris = mutableListOf(uri)
                loadPickedMeta()
                showCompressionSettings()
            }
        }

    private fun openImagePicker() { imagePickLauncher.launch("image/*") }
    private fun openPdfPicker() { pdfPickLauncher.launch("application/pdf") }
    // ================= END Stage 2/3 state =================

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
                when (screen) {
                    "settings" -> showHome()
                    "compress_settings" -> showHome()
                    "compressing" -> {
                        cancelRequested = true
                        showCompressionSettings()
                    }
                    else -> finish()
                }
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

        c.addView(
            bigCard(
                intArrayOf(0xFF12C2FF.toInt(), 0xFF2F6BFF.toInt(), 0xFF7A2BFF.toInt()), imageTile(),
                tr("Image"), tr("Compress / Resize"),
                tr("Reduce image size and adjust dimensions without losing quality."),
                0xFF2F6BFF.toInt()
            ) { openImagePicker() },
            lpm(MATCH, WRAP, 0, 22, 0, 0)
        )
        c.addView(
            bigCard(
                intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()), pdfTile(),
                tr("PDF"), tr("Compress / Resize"),
                tr("Make your PDF files smaller and fit your needs."),
                0xFFFF3D6E.toInt()
            ) { openPdfPicker() },
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
        return box1
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
    ?xml version="1.0" encoding="utf-8"?> <manifest xmlns:android="http://schemas.android.com/apk/res/a
claude-sonnet-5-high
Perfect, Part 2 mil gaya! ✅ Ab meri paas poori file hai. Maine dono parts ko join kiya, naya Stage 2 + Stage 3 code (image/pdf picker → compression settings → compress & save) integrate kiya, home screen ke cards ko wire kiya, aur back-press handling update ki.

Niche poori MainActivity.kt hai — seedha copy-paste karo, replace kar do.

Iske saath 2 chhoti cheezein bhi zaroor karo (niche instructions hain file ke baad):

Kotlin

package com.akay.compresspro

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

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

    // ================= STAGE 2 / 3 : Compression state =================
    private val ACC1 = Color.parseColor("#3B82F6")
    private val ACC2 = Color.parseColor("#8B5CF6")
    private val SUBC = Color.parseColor("#94A3B8")
    private val CARD2 = Color.parseColor("#1E293B")
    private val GREEN = Color.parseColor("#22C55E")
    private val REDC = Color.parseColor("#EF4444")

    private var pickedUris: MutableList<Uri> = mutableListOf()
    private var pickedNames: MutableList<String> = mutableListOf()
    private var pickedSizes: MutableList<Long> = mutableListOf()
    private var selectedPickIndex: Int = 0
    private var isPdfMode: Boolean = false

    private var cQuality: Int = 60
    private var origW: Int = 1280
    private var origH: Int = 720
    private var lockAspect: Boolean = true
    private var outFormat: String = "JPG"
    private var cancelRequested = false

    private val estHandler = Handler(Looper.getMainLooper())
    private var estRunnable: Runnable? = null

    private lateinit var estSizeValue: TextView
    private lateinit var estSizePct: TextView
    private lateinit var estOrigText: TextView
    private lateinit var estBarFg: View
    private lateinit var estBarContainer: LinearLayout

    private lateinit var qualityPctChip: TextView
    private lateinit var qualitySeek: SeekBar
    private lateinit var qLowBtn: TextView
    private lateinit var qMedBtn: TextView
    private lateinit var qHighBtn: TextView

    private lateinit var dimPctChip: TextView
    private lateinit var heightEdit: EditText
    private lateinit var widthEdit: EditText
    private lateinit var p25: TextView
    private lateinit var p50: TextView
    private lateinit var p75: TextView
    private lateinit var pCustom: TextView

    private lateinit var fJpg: TextView
    private lateinit var fPng: TextView
    private lateinit var fWebp: TextView
    private lateinit var fOrig: TextView

    private lateinit var progPercentText: TextView
    private lateinit var progRing: RingProgressView
    private lateinit var progTimeText: TextView
    private lateinit var progSpeedText: TextView
    private lateinit var progOrigText: TextView
    private lateinit var progCompText: TextView
    private lateinit var progFileName: TextView
    private lateinit var progThumb: ImageView
    private lateinit var progBarFg: View
    private lateinit var progBarContainer: LinearLayout
    private lateinit var step1Dot: FrameLayout
    private lateinit var step2Dot: FrameLayout
    private lateinit var step3Dot: FrameLayout
    private lateinit var step2Status: TextView
    private lateinit var step3Status: TextView
    private lateinit var progBottomBtn: LinearLayout
    private lateinit var progBottomLabel: TextView
    private lateinit var progBottomSub: TextView

    private val imagePickLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                isPdfMode = false
                pickedUris = uris.toMutableList()
                loadPickedMeta()
                showCompressionSettings()
            }
        }

    private val pdfPickLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                isPdfMode = true
                pickedUris = mutableListOf(uri)
                loadPickedMeta()
                showCompressionSettings()
            }
        }

    private fun openImagePicker() { imagePickLauncher.launch("image/*") }
    private fun openPdfPicker() { pdfPickLauncher.launch("application/pdf") }
    // ================= END Stage 2/3 state =================

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
                when (screen) {
                    "settings" -> showHome()
                    "compress_settings" -> showHome()
                    "compressing" -> {
                        cancelRequested = true
                        showCompressionSettings()
                    }
                    else -> finish()
                }
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

        c.addView(
            bigCard(
                intArrayOf(0xFF12C2FF.toInt(), 0xFF2F6BFF.toInt(), 0xFF7A2BFF.toInt()), imageTile(),
                tr("Image"), tr("Compress / Resize"),
                tr("Reduce image size and adjust dimensions without losing quality."),
                0xFF2F6BFF.toInt()
            ) { openImagePicker() },
            lpm(MATCH, WRAP, 0, 22, 0, 0)
        )
        c.addView(
            bigCard(
                intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()), pdfTile(),
                tr("PDF"), tr("Compress / Resize"),
                tr("Make your PDF files smaller and fit your needs."),
                0xFFFF3D6E.toInt()
            ) { openPdfPicker() },
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
        list.addView(row(K.SHIELD, tr("Output Quality"), tr("Set default quality for compression"), qNames[Prefs.quality()], false) {
            choose(tr("Output Quality"), qNames, Prefs.quality()) { w ->
                Prefs.setQuality(w)
                showSettings()
            }
        })
        list.addView(row(K.FOLDER, tr("Storage Location"), tr("Choose where to save files"), sNames[Prefs.storage()], false) {
            choose(tr("Storage Location"), sNames, Prefs.storage()) { w ->
                Prefs.setStorage(w)
                showSettings()
            }
        })
        list.addView(row(K.INFO, tr("About App"), tr("Version, developer and more"), "v1.0.0", true) {
            AlertDialog.Builder(this)
                .setTitle("CompressPro  v1.0.0")
                .setMessage(
                    tr("Smaller Size") + "  \u2022  " + tr("Same Quality") + "\n\n" +
                        tr("Free app, no subscription.") + "\n" + tr("Made by Akay")
                )
                .setPositiveButton(tr("Close"), null)
                .show()
        })
        c.addView(list, lpm(MATCH, WRAP, 0, 16, 0, 0))

        val list2 = cardBox()
        list2.addView(row(K.DOC, tr("Privacy Policy"), null, null, false) {
            if (PRIVACY_URL.isEmpty()) {
                toast(tr("Privacy policy link will be added soon"))
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                } catch (e: Throwable) {
                }
            }
        })
        list2.addView(row(K.STAR, tr("Rate Us"), tr("Support us with your rating"), null, true) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: Throwable) {
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    )
                } catch (e2: Throwable) {
                    toast(tr("Could not open Play Store"))
                }
            }
        })
        c.addView(list2, lpm(MATCH, WRAP, 0, 16, 0, 0))

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(c)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.addView(sv, lp(MATCH, 0, 1f))
        root.addView(navBar(1), lp(MATCH, WRAP))
        setScreen(root, P.navBg)
    }

    // ================= STAGE 2 : Compression Settings Screen =================

    private fun loadPickedMeta() {
        pickedNames = mutableListOf()
        pickedSizes = mutableListOf()
        selectedPickIndex = 0
        for (u in pickedUris) {
            var name = "file"
            var size = 0L
            contentResolver.query(u, null, null, null, null)?.use { cur ->
                val ni = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = cur.getColumnIndex(OpenableColumns.SIZE)
                if (cur.moveToFirst()) {
                    if (ni >= 0) name = cur.getString(ni) ?: name
                    if (si >= 0) size = cur.getLong(si)
                }
            }
            pickedNames.add(name)
            pickedSizes.add(size)
        }
        if (!isPdfMode && pickedUris.isNotEmpty()) {
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            contentResolver.openInputStream(pickedUris[0])?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            origW = if (opts.outWidth > 0) opts.outWidth else 1280
            origH = if (opts.outHeight > 0) opts.outHeight else 720
        }
        cQuality = when (Prefs.quality()) { 0 -> 30; 2 -> 90; else -> 60 }
        outFormat = "JPG"
        lockAspect = true
        cancelRequested = false
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        return if (kb < 1024) String.format("%.0f KB", kb)
        else String.format("%.1f MB", kb / 1024.0)
    }

    private fun sectionCard(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = rr(P.card, 18, P.cardEdge, 1)
        l.setPadding(dp(16), dp(16), dp(16), dp(16))
        return l
    }

    private fun hDivider(): View {
        val v = View(this)
        v.setBackgroundColor(P.cardEdge)
        return v
    }

    private fun gradientPill(c1: Int, c2: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(c1, c2))
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    private fun chipLabel(label: String): TextView {
        val t = tv(label, 12f, P.text, 1)
        t.gravity = Gravity.CENTER
        t.background = rr(CARD2, 12, P.cardEdge, 1)
        t.setPadding(dp(6), dp(8), dp(6), dp(8))
        return t
    }

    private fun chipSelect(chip: TextView) {
        chip.background = gradientPill(ACC1, ACC2, 12)
        chip.setTextColor(Color.WHITE)
    }

    private fun chipDeselect(chip: TextView) {
        chip.background = rr(CARD2, 12, P.cardEdge, 1)
        chip.setTextColor(P.text)
    }
private fun afterText(action: (String) -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { action(s?.toString() ?: "") }
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
    }

    private fun decodeSampledBitmap(uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            var hh = opts.outHeight; var ww = opts.outWidth
            while (hh / 2 >= reqH && ww / 2 >= reqW && reqW > 0 && reqH > 0) {
                hh /= 2; ww /= 2; sample *= 2
            }
            val opts2 = BitmapFactory.Options()
            opts2.inSampleSize = sample
            val raw = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts2)
            } ?: return null
            val tw = if (reqW > 0) reqW else raw.width
            val th = if (reqH > 0) reqH else raw.height
            if (raw.width != tw || raw.height != th) {
                val scaled = Bitmap.createScaledBitmap(raw, tw.coerceAtLeast(1), th.coerceAtLeast(1), true)
                if (scaled != raw) raw.recycle()
                scaled
            } else raw
        } catch (e: Throwable) { null }
    }

    private fun renderPdfThumb(uri: Uri): Bitmap? {
        return try {
            val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); pfd.close()
            bmp
        } catch (e: Throwable) { null }
    }

    private fun guessExt(uri: Uri): String {
        val type = contentResolver.getType(uri) ?: return "jpg"
        return when {
            type.contains("png") -> "png"
            type.contains("webp") -> "webp"
            else -> "jpg"
        }
    }

    private fun showCompressionSettings() {
        screen = "compress_settings"
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(14), dp(18), dp(24))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val back = IconView(this, K.ARROW_L, P.text)
        back.setOnClickListener { showHome() }
        top.addView(back, lp(dp(32), dp(32)))
        val tCol = LinearLayout(this)
        tCol.orientation = LinearLayout.VERTICAL
        tCol.setPadding(dp(14), 0, 0, 0)
        tCol.addView(tv(tr("Compression Settings"), 19f, P.text, 2))
        val sub = tv(tr("Adjust settings to get the best balance between quality and file size."), 11f, SUBC)
        tCol.addView(sub, lpm(MATCH, WRAP, 0, 2, 0, 0))
        top.addView(tCol, lp(0, WRAP, 1f))
        top.addView(logoTile(44), lp(dp(44), dp(44)))
        c.addView(top, lp(MATCH, WRAP))

        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val strip = LinearLayout(this)
        strip.orientation = LinearLayout.HORIZONTAL
        for (i in pickedUris.indices) {
            strip.addView(thumbItem(i), lpm(dp(72), dp(72), if (i == 0) 0 else 10, 0, 0, 0))
        }
        hs.addView(strip)
        c.addView(hs, lpm(MATCH, dp(72), 0, 16, 0, 0))

        val estCard = sectionCard()
        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.gravity = Gravity.CENTER_VERTICAL
        val lbl = tv(tr("ESTIMATED SIZE"), 11f, SUBC, 1)
        row1.addView(lbl, lp(0, WRAP, 1f))
        estSizePct = tv("-0% " + tr("Saved"), 11f, GREEN, 1)
        estSizePct.setPadding(dp(10), dp(4), dp(10), dp(4))
        estSizePct.background = rr(Color.parseColor("#143321"), 12)
        row1.addView(estSizePct)
        estCard.addView(row1)

        estSizeValue = tv("0 MB", 26f, ACC1, 2)
        estCard.addView(estSizeValue, lpm(WRAP, WRAP, 0, 8, 0, 0))
        estCard.addView(tv(tr("(After Compression)"), 11f, SUBC), lpm(WRAP, WRAP, 0, 2, 0, 0))

        estBarContainer = LinearLayout(this)
        estBarContainer.orientation = LinearLayout.HORIZONTAL
        estBarContainer.background = rr(CARD2, 6)
        estBarFg = View(this)
        estBarFg.background = rr(ACC1, 6)
        estBarContainer.addView(estBarFg, lp(0, dp(6), 0.2f))
        val sp1 = View(this)
        estBarContainer.addView(sp1, lp(0, dp(6), 0.8f))
        estCard.addView(estBarContainer, lpm(MATCH, dp(6), 0, 12, 0, 0))

        estOrigText = tv(tr("Original") + ": 0 MB", 11f, SUBC)
        estOrigText.gravity = Gravity.END
        estCard.addView(estOrigText, lpm(MATCH, WRAP, 0, 6, 0, 0))
        c.addView(estCard, lpm(MATCH, WRAP, 0, 16, 0, 0))

        c.addView(buildQualityCard(), lpm(MATCH, WRAP, 0, 16, 0, 0))
        c.addView(buildDimensionsCard(), lpm(MATCH, WRAP, 0, 16, 0, 0))
        if (!isPdfMode) {
            c.addView(buildFormatCard(), lpm(MATCH, WRAP, 0, 16, 0, 0))
        }
        c.addView(buildCompressButton(), lpm(MATCH, dp(58), 0, 20, 0, 0))

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(c)

        root.addView(sv, lp(MATCH, 0, 1f))
        root.addView(navBar(0), lp(MATCH, WRAP))
        setScreen(root, P.navBg)

        refreshEstimate()
    }

    private fun thumbItem(i: Int): View {
        val f = FrameLayout(this)
        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.background = rr(CARD2, 14, if (i == selectedPickIndex) ACC1 else P.cardEdge, if (i == selectedPickIndex) 2 else 1)
        thread {
            val bmp = if (isPdfMode) renderPdfThumb(pickedUris[i]) else decodeSampledBitmap(pickedUris[i], 160, 160)
            runOnUiThread { if (bmp != null) iv.setImageBitmap(bmp) }
        }
        f.addView(iv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (i == selectedPickIndex) {
            val chk = FrameLayout(this)
            chk.background = rr(ACC1, 20)
            val cv = tv("\u2713", 11f, Color.WHITE, 1)
            cv.gravity = Gravity.CENTER
            chk.addView(cv, FrameLayout.LayoutParams(dp(18), dp(18)))
            val lpChk = FrameLayout.LayoutParams(dp(18), dp(18))
            lpChk.gravity = Gravity.BOTTOM or Gravity.END
            lpChk.setMargins(0, 0, dp(4), dp(4))
            f.addView(chk, lpChk)
        }
        f.setOnClickListener {
            selectedPickIndex = i
            if (!isPdfMode) {
                val opts = BitmapFactory.Options()
                opts.inJustDecodeBounds = true
                contentResolver.openInputStream(pickedUris[i])?.use { BitmapFactory.decodeStream(it, null, opts) }
                origW = if (opts.outWidth > 0) opts.outWidth else origW
                origH = if (opts.outHeight > 0) opts.outHeight else origH
            }
            showCompressionSettings()
        }
        return f
    }

    private fun buildQualityCard(): LinearLayout {
        val l = sectionCard()
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val icWrap = FrameLayout(this)
        icWrap.background = rr(ACC2, 10)
        val icTxt = tv("\u2699", 16f, Color.WHITE, 1)
        icTxt.gravity = Gravity.CENTER
        icWrap.addView(icTxt, FrameLayout.LayoutParams(dp(34), dp(34)))
        head.addView(icWrap, lp(dp(34), dp(34)))
        val tcol = LinearLayout(this)
        tcol.orientation = LinearLayout.VERTICAL
        tcol.setPadding(dp(10), 0, 0, 0)
        tcol.addView(tv(tr("Quality"), 15f, P.text, 1))
        tcol.addView(tv(tr("Adjust the compression quality."), 11f, SUBC))
        head.addView(tcol, lp(0, WRAP, 1f))
        qualityPctChip = tv("$cQuality%", 12f, Color.WHITE, 1)
        qualityPctChip.setPadding(dp(10), dp(4), dp(10), dp(4))
        qualityPctChip.background = gradientPill(ACC1, ACC2, 14)
        head.addView(qualityPctChip)
        l.addView(head)

        qualitySeek = SeekBar(this)
        qualitySeek.max = 100
        qualitySeek.progress = cQuality
        qualitySeek.progressTintList = ColorStateList.valueOf(ACC1)
        qualitySeek.thumbTintList = ColorStateList.valueOf(ACC2)
        qualitySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                cQuality = p
                qualityPctChip.text = "$p%"
                highlightQualityChips()
                refreshEstimate()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        l.addView(qualitySeek, lpm(MATCH, WRAP, 0, 8, 0, 0))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        qLowBtn = chipLabel(tr("Low (30%)"))
        qMedBtn = chipLabel(tr("Medium (60%)"))
        qHighBtn = chipLabel(tr("High (90%)"))
        qLowBtn.setOnClickListener { setQuality(30) }
        qMedBtn.setOnClickListener { setQuality(60) }
        qHighBtn.setOnClickListener { setQuality(90) }
        row.addView(qLowBtn, LinearLayout.LayoutParams(0, dp(38), 1f))
        row.addView(qMedBtn, LinearLayout.LayoutParams(0, dp(38), 1f).also { it.marginStart = dp(8) })
        row.addView(qHighBtn, LinearLayout.LayoutParams(0, dp(38), 1f).also { it.marginStart = dp(8) })
        l.addView(row, lpm(MATCH, dp(38), 0, 10, 0, 0))
        highlightQualityChips()
        return l
    }

    private fun setQuality(v: Int) {
        cQuality = v
        qualitySeek.progress = v
        qualityPctChip.text = "$v%"
        highlightQualityChips()
        refreshEstimate()
    }

    private fun highlightQualityChips() {
        listOf(qLowBtn, qMedBtn, qHighBtn).forEach { chipDeselect(it) }
        when (cQuality) { 30 -> chipSelect(qLowBtn); 60 -> chipSelect(qMedBtn); 90 -> chipSelect(qHighBtn) }
    }

    private fun buildDimensionsCard(): LinearLayout {
        val l = sectionCard()
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val icWrap = FrameLayout(this)
        icWrap.background = rr(Color.parseColor("#0D9488"), 10)
        val icTxt = tv("\u2922", 16f, Color.WHITE, 1)
        icTxt.gravity = Gravity.CENTER
        icWrap.addView(icTxt, FrameLayout.LayoutParams(dp(34), dp(34)))
        head.addView(icWrap, lp(dp(34), dp(34)))
        val tcol = LinearLayout(this)
        tcol.orientation = LinearLayout.VERTICAL
        tcol.setPadding(dp(10), 0, 0, 0)
        tcol.addView(tv(tr("Dimensions"), 15f, P.text, 1))
        tcol.addView(tv(tr("Set your desired width and height."), 11f, SUBC))
        head.addView(tcol, lp(0, WRAP, 1f))
        dimPctChip = tv("100%", 12f, Color.WHITE, 1)
        dimPctChip.setPadding(dp(10), dp(4), dp(10), dp(4))
        dimPctChip.background = rr(ACC1, 14)
        head.addView(dimPctChip)
        l.addView(head)

        if (isPdfMode) {
            val note = tv(tr("Dimensions apply to image files only."), 11f, SUBC)
            l.addView(note, lpm(MATCH, WRAP, 0, 10, 0, 0))
            return l
        }

        val hw = LinearLayout(this)
        hw.orientation = LinearLayout.HORIZONTAL
        val hCol = LinearLayout(this); hCol.orientation = LinearLayout.VERTICAL
        hCol.addView(tv(tr("Height"), 11f, SUBC))
        heightEdit = EditText(this)
        heightEdit.setText(origH.toString())
        heightEdit.inputType = InputType.TYPE_CLASS_NUMBER
        heightEdit.background = rr(CARD2, 10, P.cardEdge, 1)
        heightEdit.setPadding(dp(10), dp(8), dp(10), dp(8))
        heightEdit.setTextColor(P.text)
        hCol.addView(heightEdit, lpm(MATCH, dp(42), 0, 4, 0, 0))
        hw.addView(hCol, lp(0, WRAP, 1f))

        val xLbl = tv("\u00D7", 16f, SUBC)
        xLbl.gravity = Gravity.CENTER
        hw.addView(xLbl, lpm(dp(24), dp(42), 6, 18, 6, 0))

        val wCol = LinearLayout(this); wCol.orientation = LinearLayout.VERTICAL
        wCol.addView(tv(tr("Width"), 11f, SUBC))
        widthEdit = EditText(this)
        widthEdit.setText(origW.toString())
        widthEdit.inputType = InputType.TYPE_CLASS_NUMBER
        widthEdit.background = rr(CARD2, 10, P.cardEdge, 1)
        widthEdit.setPadding(dp(10), dp(8), dp(10), dp(8))
        widthEdit.setTextColor(P.text)
        wCol.addView(widthEdit, lpm(MATCH, dp(42), 0, 4, 0, 0))
        hw.addView(wCol, lp(0, WRAP, 1f))
        l.addView(hw, lpm(MATCH, WRAP, 0, 12, 0, 0))

        val lockRow = LinearLayout(this)
        lockRow.gravity = Gravity.CENTER_VERTICAL
        lockRow.addView(tv(tr("Lock Aspect Ratio"), 11f, SUBC), lp(0, WRAP, 1f))
        val sw = Switch(this)
        sw.isChecked = lockAspect
        sw.trackTintList = ColorStateList.valueOf(ACC1)
        sw.setOnCheckedChangeListener { _, chk -> lockAspect = chk }
        lockRow.addView(sw)
        l.addView(lockRow, lpm(MATCH, WRAP, 0, 8, 0, 0))

        val pr = LinearLayout(this)
        pr.orientation = LinearLayout.HORIZONTAL
        p25 = chipLabel("25%"); p50 = chipLabel("50%"); p75 = chipLabel("75%"); pCustom = chipLabel(tr("Custom"))
        p25.setOnClickListener { applyDimPercent(25) }
        p50.setOnClickListener { applyDimPercent(50) }
        p75.setOnClickListener { applyDimPercent(75) }
        pCustom.setOnClickListener { setDimChipSelected(pCustom) }
        pr.addView(p25, LinearLayout.LayoutParams(0, dp(36), 1f))
        pr.addView(p50, LinearLayout.LayoutParams(0, dp(36), 1f).also { it.marginStart = dp(8) })
        pr.addView(p75, LinearLayout.LayoutParams(0, dp(36), 1f).also { it.marginStart = dp(8) })
        pr.addView(pCustom, LinearLayout.LayoutParams(0, dp(36), 1f).also { it.marginStart = dp(8) })
        l.addView(pr, lpm(MATCH, dp(36), 0, 10, 0, 0))

        heightEdit.addTextChangedListener(afterText { s ->
            if (lockAspect && origH > 0) {
                val h = s.toIntOrNull() ?: 0
                if (h > 0) {
                    val w = (h.toFloat() * origW / origH).toInt()
                    if (widthEdit.text.toString() != w.toString()) widthEdit.setText(w.toString())
                }
            }
            updateDimChip()
            refreshEstimate()
        })
        widthEdit.addTextChangedListener(afterText { s ->
            if (lockAspect && origW > 0) {
                val w = s.toIntOrNull() ?: 0
                if (w > 0) {
                    val h = (w.toFloat() * origH / origW).toInt()
                    if (heightEdit.text.toString() != h.toString()) heightEdit.setText(h.toString())
                }
            }
            updateDimChip()
            refreshEstimate()
        })
        return l
    }

    private fun updateDimChip() {
        if (isPdfMode || !::widthEdit.isInitialized) return
        val w = widthEdit.text.toString().toIntOrNull() ?: origW
        val pct = if (origW > 0) (w * 100 / origW) else 100
        dimPctChip.text = "$pct%"
    }

    private fun applyDimPercent(pct: Int) {
        val w = (origW * pct / 100f).toInt().coerceAtLeast(1)
        val h = (origH * pct / 100f).toInt().coerceAtLeast(1)
        widthEdit.setText(w.toString())
        heightEdit.setText(h.toString())
        setDimChipSelected(when (pct) { 25 -> p25; 50 -> p50; 75 -> p75; else -> pCustom })
        updateDimChip()
        refreshEstimate()
    }

    private fun setDimChipSelected(sel: TextView) {
        listOf(p25, p50, p75, pCustom).forEach { chipDeselect(it) }
        chipSelect(sel)
    }

    private fun buildFormatCard(): LinearLayout {
        val l = sectionCard()
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val icWrap = FrameLayout(this)
        icWrap.background = rr(ACC2, 10)
        val icTxt = tv("\uD83D\uDDBC", 14f, Color.WHITE, 1)
        icTxt.gravity = Gravity.CENTER
        icWrap.addView(icTxt, FrameLayout.LayoutParams(dp(34), dp(34)))
        head.addView(icWrap, lp(dp(34), dp(34)))
        val tcol = LinearLayout(this)
        tcol.orientation = LinearLayout.VERTICAL
        tcol.setPadding(dp(10), 0, 0, 0)
        tcol.addView(tv(tr("Format"), 15f, P.text, 1))
        tcol.addView(tv(tr("Choose output format for your image."), 11f, SUBC))
        head.addView(tcol, lp(0, WRAP, 1f))
        l.addView(head)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        fJpg = chipLabel("JPG"); fPng = chipLabel("PNG"); fWebp = chipLabel("WEBP"); fOrig = chipLabel(tr("Original"))
        fJpg.setOnClickListener { setFormat("JPG") }
        fPng.setOnClickListener { setFormat("PNG") }
        fWebp.setOnClickListener { setFormat("WEBP") }
        fOrig.setOnClickListener { setFormat("ORIGINAL") }
        row.addView(fJpg, LinearLayout.LayoutParams(0, dp(38), 1f))
        row.addView(fPng, LinearLayout.LayoutParams(0, dp(38), 1f).also { it.marginStart = dp(8) })
        row.addView(fWebp, LinearLayout.LayoutParams(0, dp(38), 1f).also { it.marginStart = dp(8) })
        row.addView(fOrig, LinearLayout.LayoutParams(0, dp(38), 1f).also { it.marginStart = dp(8) })
        l.addView(row, lpm(MATCH, dp(38), 0, 10, 0, 0))
        setFormat(outFormat)
        return l
    }

    private fun setFormat(f: String) {
        outFormat = f
        listOf(fJpg, fPng, fWebp, fOrig).forEach { chipDeselect(it) }
        when (f) { "JPG" -> chipSelect(fJpg); "PNG" -> chipSelect(fPng); "WEBP" -> chipSelect(fWebp); else -> chipSelect(fOrig) }
        refreshEstimate()
    }

    private fun buildCompressButton(): View {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER
        b.background = gradientPill(ACC1, ACC2, 29)
        b.addView(tv("\u2B07", 18f, Color.WHITE, 1), lpm(WRAP, WRAP, 0, 0, 10, 0))
        val tcol = LinearLayout(this)
        tcol.orientation = LinearLayout.VERTICAL
        tcol.gravity = Gravity.CENTER
        tcol.addView(tv(tr(if (isPdfMode) "Compress PDF" else "Compress Image"), 16f, Color.WHITE, 1))
        tcol.addView(tv(tr("Tap to start compression"), 10f, Color.WHITE, 0))
        b.addView(tcol)
        b.setOnClickListener { showCompressingProgress() }
        return b
    }

    private fun refreshEstimate() {
        if (!::estSizeValue.isInitialized) return
        estRunnable?.let { estHandler.removeCallbacks(it) }
        val r = Runnable { computeEstimate() }
        estRunnable = r
        estHandler.postDelayed(r, 250)
    }

    private fun computeEstimate() {
        thread {
            try {
                val origSize = pickedSizes.getOrElse(selectedPickIndex) { 0L }
                val estSize: Long = if (isPdfMode) {
                    val qf = cQuality / 100f
                    (origSize * (0.25f + 0.65f * qf)).toLong().coerceAtLeast(10_000)
                } else {
                    val uri = pickedUris[selectedPickIndex]
                    val w = if (::widthEdit.isInitialized) (widthEdit.text.toString().toIntOrNull() ?: origW) else origW
                    val h = if (::heightEdit.isInitialized) (heightEdit.text.toString().toIntOrNull() ?: origH) else origH
                    val bmp = decodeSampledBitmap(uri, w, h)
                    if (bmp != null) {
                        val baos = ByteArrayOutputStream()
                        val fmt = when (outFormat) {
                            "PNG" -> Bitmap.CompressFormat.PNG
                            "WEBP" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                            else -> Bitmap.CompressFormat.JPEG
                        }
                        bmp.compress(fmt, cQuality.coerceIn(1, 100), baos)
                        val s = baos.size().toLong()
                        bmp.recycle()
                        s
                    } else origSize
                }
                runOnUiThread { updateEstimateUI(origSize, estSize) }
            } catch (e: Throwable) {
                val fallback = pickedSizes.getOrElse(selectedPickIndex) { 0L }
                runOnUiThread { updateEstimateUI(fallback, fallback) }
            }
        }
    }

    private fun updateEstimateUI(orig: Long, est: Long) {
        if (!::estSizeValue.isInitialized) return
        estSizeValue.text = formatSize(est)
        estOrigText.text = tr("Original") + ": " + formatSize(orig)
        val savedPct = if (orig > 0) (100 - (est * 100 / orig)).toInt().coerceIn(0, 99) else 0
        estSizePct.text = "-$savedPct% " + tr("Saved")
        val frac = if (orig > 0) (est.toFloat() / orig.toFloat()).coerceIn(0.02f, 1f) else 1f
        (estBarFg.layoutParams as LinearLayout.LayoutParams).weight = frac
        (estBarContainer.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight = 1f - frac
        estBarContainer.requestLayout()
    }

    // ================= STAGE 3 : Progress Screen =================

    private fun showCompressingProgress() {
        screen = "compressing"
        cancelRequested = false
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(14), dp(18), dp(24))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val back = IconView(this, K.ARROW_L, P.text)
        back.setOnClickListener { cancelRequested = true; showCompressionSettings() }
        top.addView(back, lp(dp(32), dp(32)))
        val tCol = LinearLayout(this)
        tCol.orientation = LinearLayout.VERTICAL
        tCol.setPadding(dp(14), 0, 0, 0)
        tCol.addView(tv(tr(if (isPdfMode) "Compressing PDF" else "Compressing Image"), 18f, P.text, 2))
        tCol.addView(tv(tr("Please wait while we compress your file..."), 11f, SUBC))
        top.addView(tCol, lp(0, WRAP, 1f))
        val hqChip = LinearLayout(this)
        hqChip.background = rr(CARD2, 16, P.cardEdge, 1)
        hqChip.setPadding(dp(10), dp(6), dp(10), dp(6))
        hqChip.addView(tv("\u26A1 " + tr("High Quality"), 10f, ACC1, 1))
        top.addView(hqChip)
        c.addView(top, lp(MATCH, WRAP))

        val statRow = LinearLayout(this)
        statRow.orientation = LinearLayout.HORIZONTAL
        statRow.gravity = Gravity.CENTER_VERTICAL
        val timeCard = sectionCard()
        timeCard.addView(tv(tr("Estimated Time"), 10f, SUBC))
        progTimeText = tv("00:10", 18f, P.text, 2)
        timeCard.addView(progTimeText, lpm(WRAP, WRAP, 0, 4, 0, 0))
        timeCard.addView(tv(tr("remaining"), 10f, SUBC))
        statRow.addView(timeCard, lp(0, WRAP, 1f))

        val ringWrap = FrameLayout(this)
        progRing = RingProgressView(this, ACC1, CARD2)
        ringWrap.addView(progRing, FrameLayout.LayoutParams(dp(120), dp(120)))
        progPercentText = tv("0%", 20f, P.text, 2)
        val ringTxtLp = FrameLayout.LayoutParams(dp(120), dp(120))
        ringTxtLp.gravity = Gravity.CENTER
        val ringTxtWrap = FrameLayout(this)
        ringTxtWrap.addView(
            progPercentText,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.CENTER }
        )
        ringWrap.addView(ringTxtWrap, ringTxtLp)
        statRow.addView(ringWrap, lpm(dp(120), dp(120), 10, 0, 10, 0))

        val speedCard = sectionCard()
        speedCard.addView(tv(tr("Processing"), 10f, SUBC))
        progSpeedText = tv("0 MB/s", 16f, P.text, 2)
        speedCard.addView(progSpeedText, lpm(WRAP, WRAP, 0, 4, 0, 0))
        statRow.addView(speedCard, lp(0, WRAP, 1f))
        c.addView(statRow, lpm(MATCH, WRAP, 0, 18, 0, 0))

        val sizeCard = sectionCard()
        val sr = LinearLayout(this)
        sr.orientation = LinearLayout.HORIZONTAL
        sr.gravity = Gravity.CENTER_VERTICAL
        progThumb = ImageView(this)
        progThumb.scaleType = ImageView.ScaleType.CENTER_CROP
        progThumb.background = rr(CARD2, 10, P.cardEdge, 1)
        sr.addView(progThumb, lp(dp(50), dp(50)))
        val oc = LinearLayout(this); oc.orientation = LinearLayout.VERTICAL
        oc.setPadding(dp(10), 0, 0, 0)
        oc.addView(tv(tr("Original Size"), 10f, SUBC))
        progOrigText = tv(formatSize(pickedSizes.getOrElse(0) { 0L }), 15f, P.text, 1)
        oc.addView(progOrigText, lpm(WRAP, WRAP, 0, 2, 0, 0))
        sr.addView(oc, lp(0, WRAP, 1f))
        val ccCol = LinearLayout(this); ccCol.orientation = LinearLayout.VERTICAL
        ccCol.gravity = Gravity.END
        ccCol.addView(tv(tr("Compressed Size"), 10f, SUBC))
        progCompText = tv("-- MB", 15f, P.text, 1)
        ccCol.addView(progCompText, lpm(WRAP, WRAP, 0, 2, 0, 0))
        sr.addView(ccCol)
        sizeCard.addView(sr)

        progBarContainer = LinearLayout(this)
        progBarContainer.orientation = LinearLayout.HORIZONTAL
        progBarContainer.background = rr(CARD2, 5)
        progBarFg = View(this)
        progBarFg.background = rr(ACC1, 5)
        progBarContainer.addView(progBarFg, LinearLayout.LayoutParams(0, dp(5), 0f))
        val sp2 = View(this)
        progBarContainer.addView(sp2, LinearLayout.LayoutParams(0, dp(5), 1f))
        sizeCard.addView(progBarContainer, lpm(MATCH, dp(5), 0, 12, 0, 0))
        progFileName = tv(pickedNames.getOrElse(0) { "file" }, 10f, SUBC)
        sizeCard.addView(progFileName, lpm(WRAP, WRAP, 0, 8, 0, 0))
        c.addView(sizeCard, lpm(MATCH, WRAP, 0, 16, 0, 0))

        thread {
            val bmp = if (isPdfMode) renderPdfThumb(pickedUris[0]) else decodeSampledBitmap(pickedUris[0], 100, 100)
            runOnUiThread { if (bmp != null) progThumb.setImageBitmap(bmp) }
        }

        val stepsCard = sectionCard()
        val s1 = stepRow(tr("Reading file"), tr("Analyzing file and preparing..."))
        step1Dot = s1.second
        stepsCard.addView(s1.first)
        stepsCard.addView(hDivider(), lp(MATCH, dp(1)))
        val s2 = stepRow(tr("Compressing"), tr("Reducing file size and optimizing quality..."))
        step2Dot = s2.second
        step2Status = s2.third
        stepsCard.addView(s2.first)
        stepsCard.addView(hDivider(), lp(MATCH, dp(1)))
        val s3 = stepRow(tr("Saving file"), tr("Finalizing and saving to your device..."))
        step3Dot = s3.second
        step3Status = s3.third
        stepsCard.addView(s3.first)
        c.addView(stepsCard, lpm(MATCH, WRAP, 0, 16, 0, 0))

        progBottomBtn = LinearLayout(this)
        progBottomBtn.orientation = LinearLayout.HORIZONTAL
        progBottomBtn.gravity = Gravity.CENTER
        progBottomBtn.background = gradientPill(ACC1, ACC2, 29)
        val bcol = LinearLayout(this); bcol.orientation = LinearLayout.VERTICAL; bcol.gravity = Gravity.CENTER
        progBottomLabel = tv(tr("Please Wait..."), 15f, Color.WHITE, 1)
        progBottomSub = tv(tr("This may take a few seconds"), 10f, Color.WHITE, 0)
        bcol.addView(progBottomLabel); bcol.addView(progBottomSub)
        progBottomBtn.addView(bcol)
        c.addView(progBottomBtn, lpm(MATCH, dp(56), 0, 18, 0, 0))

        val cancelBtn = LinearLayout(this)
        cancelBtn.gravity = Gravity.CENTER
        cancelBtn.background = rr(Color.TRANSPARENT, 29, REDC, 1)
        cancelBtn.addView(tv(tr("Cancel"), 14f, REDC, 1))
        cancelBtn.setOnClickListener { cancelRequested = true; showCompressionSettings() }
        c.addView(cancelBtn, lpm(MATCH, dp(50), 0, 10, 0, 0))

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(c)
        root.addView(sv, lp(MATCH, 0, 1f))
        setScreen(root, P.navBg)

        runCompressionTask()
    }

    private fun stepRow(title: String, sub: String): Triple<View, FrameLayout, TextView> {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(8), 0, dp(8))
        val dot = FrameLayout(this)
        dot.background = rr(CARD2, 16, P.cardEdge, 1)
        row.addView(dot, lp(dp(28), dp(28)))
        val tcol = LinearLayout(this); tcol.orientation = LinearLayout.VERTICAL
        tcol.setPadding(dp(10), 0, 0, 0)
        tcol.addView(tv(title, 13f, P.text, 1))
        tcol.addView(tv(sub, 10f, SUBC))
        row.addView(tcol, lp(0, WRAP, 1f))
        val status = tv(tr("Waiting"), 11f, SUBC)
        row.addView(status)
        return Triple(row, dot, status)
    }

    private fun setStepDone(dot: FrameLayout, status: TextView?) {
        dot.removeAllViews()
        dot.background = rr(ACC1, 16, P.cardEdge, 1)
        val txt = tv("\u2713", 12f, Color.WHITE, 1)
        txt.gravity = Gravity.CENTER
        dot.addView(txt, FrameLayout.LayoutParams(dp(28), dp(28)))
        status?.text = tr("Done")
    }

    private fun setStepRunning(status: TextView?) { status?.text = tr("In Progress") }

    private fun updateCurrentFileUI(idx: Int) {
        progOrigText.text = formatSize(pickedSizes.getOrElse(idx) { 0L })
        progFileName.text = pickedNames.getOrElse(idx) { "file" }
        thread {
            val bmp = if (isPdfMode) renderPdfThumb(pickedUris[idx]) else decodeSampledBitmap(pickedUris[idx], 100, 100)
            runOnUiThread { if (bmp != null) progThumb.setImageBitmap(bmp) }
        }
    }
private fun updateProgressUI(pct: Int, orig: Long, comp: Long, startTime: Long) {
        val p = pct.coerceIn(0, 100)
        progPercentText.text = "$p%"
        progRing.setProgress(p)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        val remain = if (p > 0) ((elapsed / p) * (100 - p)).toInt().coerceAtLeast(0) else 10
        progTimeText.text = String.format("00:%02d", remain.coerceAtMost(99))
        val speed = if (elapsed > 0) (orig / 1024f / 1024f) / elapsed else 0f
        progSpeedText.text = String.format("%.1f MB/s", speed)
        if (comp > 0) progCompText.text = formatSize(comp)
        val frac = p / 100f
        (progBarFg.layoutParams as LinearLayout.LayoutParams).weight = frac
        (progBarContainer.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight = 1f - frac
        progBarContainer.requestLayout()
    }

    private fun compressImage(uri: Uri, quality: Int, w: Int, h: Int, format: String): File {
        val bmp = decodeSampledBitmap(uri, w, h) ?: throw Exception("decode failed")
        val ext = when (format) { "PNG" -> "png"; "WEBP" -> "webp"; "ORIGINAL" -> guessExt(uri); else -> "jpg" }
        val outFile = File(cacheDir, "compressed_${System.currentTimeMillis()}_${(0..999).random()}.$ext")
        FileOutputStream(outFile).use { fos ->
            val cf = when (format) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            bmp.compress(cf, quality.coerceIn(1, 100), fos)
        }
        bmp.recycle()
        return outFile
    }

    private fun compressPdf(uri: Uri, quality: Int, onProgress: (Int) -> Unit): File {
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: throw Exception("cannot open pdf")
        val renderer = PdfRenderer(pfd)
        val outDoc = PdfDocument()
        val pageCount = renderer.pageCount.coerceAtLeast(1)
        for (i in 0 until renderer.pageCount) {
            if (cancelRequested) break
            val page = renderer.openPage(i)
            val scale = when { quality >= 80 -> 2.0f; quality >= 50 -> 1.5f; else -> 1.0f }
            val bw = (page.width * scale).toInt().coerceAtLeast(1)
            val bh = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), baos)
            val compBytes = baos.toByteArray()
            val compBmp = BitmapFactory.decodeByteArray(compBytes, 0, compBytes.size)

            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i).create()
            val outPage = outDoc.startPage(pageInfo)
            val destRect = Rect(0, 0, page.width, page.height)
            outPage.canvas.drawBitmap(compBmp, null, destRect, null)
            outDoc.finishPage(outPage)

            bmp.recycle(); compBmp.recycle(); page.close()
            onProgress(((i + 1) * 90 / pageCount))
        }
        renderer.close(); pfd.close()
        val outFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { outDoc.writeTo(it) }
        outDoc.close()
        return outFile
    }

    private fun saveToDevice(file: File, isPdf: Boolean): Uri? {
        val mime = if (isPdf) "application/pdf" else when (outFormat) {
            "PNG" -> "image/png"; "WEBP" -> "image/webp"; else -> "image/jpeg"
        }
        val displayName = file.name
        return try {
            if (Prefs.storage() == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CompressPro")
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    uri?.let { u -> contentResolver.openOutputStream(u)?.use { os -> file.inputStream().use { it.copyTo(os) } } }
                    uri
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CompressPro")
                    if (!dir.exists()) dir.mkdirs()
                    val dest = File(dir, displayName)
                    file.copyTo(dest, overwrite = true)
                    FileProvider.getUriForFile(this, "$packageName.fileprovider", dest)
                }
            } else {
                if (!isPdf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CompressPro")
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let { u -> contentResolver.openOutputStream(u)?.use { os -> file.inputStream().use { it.copyTo(os) } } }
                    uri
                } else {
                    val dir = getExternalFilesDir(if (isPdf) Environment.DIRECTORY_DOCUMENTS else Environment.DIRECTORY_PICTURES)
                    if (dir != null && !dir.exists()) dir.mkdirs()
                    val dest = File(dir, displayName)
                    file.copyTo(dest, overwrite = true)
                    FileProvider.getUriForFile(this, "$packageName.fileprovider", dest)
                }
            }
        } catch (e: Throwable) { null }
    }

    private fun runCompressionTask() {
        thread {
            try {
                val total = pickedUris.size.coerceAtLeast(1)
                var totalOrig = 0L
                var totalComp = 0L
                val savedUris = mutableListOf<Uri>()
                val startTime = System.currentTimeMillis()

                runOnUiThread { setStepRunning(null) }
                Thread.sleep(250)
                runOnUiThread { setStepDone(step1Dot, null) }
                runOnUiThread { setStepRunning(step2Status) }

                for ((idx, uri) in pickedUris.withIndex()) {
                    if (cancelRequested) return@thread
                    val origSize = pickedSizes.getOrElse(idx) { 0L }
                    totalOrig += origSize
                    runOnUiThread { updateCurrentFileUI(idx) }

                    var outFile: File? = null
                    if (isPdfMode) {
                        outFile = compressPdf(uri, cQuality) { p ->
                            val overall = (idx * 100 + p) / total
                            runOnUiThread { updateProgressUI(overall, totalOrig, totalComp, startTime) }
                        }
                    } else {
                        val w = if (::widthEdit.isInitialized) (widthEdit.text.toString().toIntOrNull() ?: origW) else origW
                        val h = if (::heightEdit.isInitialized) (heightEdit.text.toString().toIntOrNull() ?: origH) else origH
                        for (p in 20..80 step 20) {
                            if (cancelRequested) return@thread
                            Thread.sleep(35)
                            val overall = (idx * 100 + p) / total
                            runOnUiThread { updateProgressUI(overall, totalOrig, totalComp, startTime) }
                        }
                        outFile = compressImage(uri, cQuality, w, h, outFormat)
                    }
                    if (outFile == null) continue
                    totalComp += outFile.length()
                    val sUri = saveToDevice(outFile, isPdfMode)
                    sUri?.let { savedUris.add(it) }
                    val overall = ((idx + 1) * 100) / total
                    runOnUiThread { updateProgressUI(overall, totalOrig, totalComp, startTime) }
                }

                runOnUiThread { setStepDone(step2Dot, step2Status); setStepRunning(step3Status) }
                Thread.sleep(200)
                runOnUiThread {
                    setStepDone(step3Dot, step3Status)
                    updateProgressUI(100, totalOrig, totalComp, startTime)
                    onCompressionFinished(totalOrig, totalComp, savedUris)
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    toast(tr("Compression failed") + ": " + (e.message ?: ""))
                    showCompressionSettings()
                }
            }
        }
    }

    private fun onCompressionFinished(orig: Long, comp: Long, savedUris: List<Uri>) {
        progBottomLabel.text = tr("Completed")
        progBottomSub.text = formatSize(comp) + " \u2022 " + tr("Saved to device")
        progBottomBtn.setOnClickListener { showHome() }
        val savedPct = if (orig > 0) (100 - (comp * 100 / orig)).toInt().coerceIn(0, 99) else 0
        AlertDialog.Builder(this)
            .setTitle(tr("Success"))
            .setMessage(
                tr("File compressed successfully!") + "\n\n" +
                    tr("Original") + ": " + formatSize(orig) + "\n" +
                    tr("Compressed") + ": " + formatSize(comp) + " (-$savedPct%)"
            )
            .setPositiveButton(tr("Open")) { _, _ ->
                try {
                    savedUris.firstOrNull()?.let {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.setDataAndType(it, if (isPdfMode) "application/pdf" else "image/*")
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(i)
                    }
                } catch (e: Throwable) { toast(tr("Could not open file")) }
            }
            .setNegativeButton(tr("Done")) { _, _ -> showHome() }
            .show()
    }
}

class RingProgressView(
    ctx: android.content.Context,
    private val fgColor: Int,
    private val bgColor: Int
) : View(ctx) {
    private var progress = 0
    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 16f; color = bgColor; strokeCap = Paint.Cap.ROUND
    }
    private val paintFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 16f; color = fgColor; strokeCap = Paint.Cap.ROUND
    }
    fun setProgress(p: Int) { progress = p; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = paintBg.strokeWidth
        val rect = RectF(pad, pad, width - pad, height - pad)
        canvas.drawArc(rect, 0f, 360f, false, paintBg)
        canvas.drawArc(rect, -90f, 360f * (progress / 100f), false, paintFg)
    }
}
    
    

    
    
