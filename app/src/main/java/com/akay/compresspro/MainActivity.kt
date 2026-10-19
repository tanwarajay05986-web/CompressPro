package com.akay.compresspro

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
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
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var screen = "home"
    private var activeTab = "home"
    private lateinit var prefs: SharedPreferences
    private var darkMode = false
    private lateinit var root: FrameLayout

    private var qualityPercent = 60
    private var selectedFormat = "JPG"
    private var lockAspect = true
    private var heightPx = "720"
    private var widthPx = "1280"
    private var selectedThumbnail = 0

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted() }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Int): Float = v * resources.displayMetrics.density
    private fun tr(s: String): String = s

    private fun lp(w: Int, h: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(w, h)
    }

    private fun lpm(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.setMargins(dp(l), dp(t), dp(r), dp(b))
        return p
    }

    private fun rr(color: Int, radius: Float): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = radius
        return d
    }

    private fun grad(colors: IntArray, radius: Float): GradientDrawable {
        val d = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        d.cornerRadius = radius
        return d
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun gradText(text: String, size: Float, colors: IntArray): TextView {
        val t = tv(text, size, Color.WHITE, true)
        t.post {
            val width = t.paint.measureText(text)
            val shader = LinearGradient(0f, 0f, width, t.textSize, colors, null, Shader.TileMode.CLAMP)
            t.paint.shader = shader
            t.invalidate()
        }
        return t
    }

    private fun colBg(): Int = if (darkMode) 0xFF0E0E12.toInt() else 0xFFFFFFFF.toInt()
    private fun colCard(): Int = if (darkMode) 0xFF1B1B20.toInt() else 0xFFF4F4F7.toInt()
    private fun colText(): Int = if (darkMode) 0xFFFFFFFF.toInt() else 0xFF15151A.toInt()
    private fun colTextSub(): Int = if (darkMode) 0xFFAAAAB2.toInt() else 0xFF6D6D78.toInt()
    private fun colNavBg(): Int = if (darkMode) 0xFF18181C.toInt() else 0xFFF6F6F9.toInt()
    private fun colNavInactive(): Int = if (darkMode) 0xFF75757D.toInt() else 0xFFAFAFB8.toInt()
    private fun colDivider(): Int = if (darkMode) 0xFF2A2A30.toInt() else 0xFFE6E6EB.toInt()
    private fun colAccent(): Int = 0xFFFF3D6E.toInt()

    private fun applyStatusBarStyle() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !darkMode
    }

    private fun permList(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun hasPerm(): Boolean {
        return permList().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun permBlocked(): Boolean {
        return permList().any { !shouldShowRequestPermissionRationale(it) } && !hasPerm()
    }

    private fun granted() {
        if (hasPerm()) setScreen("home")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("cp_settings", MODE_PRIVATE)
        darkMode = prefs.getBoolean("dark_mode", false)
        root = FrameLayout(this)
        setContentView(root)
        applyStatusBarStyle()
        if (hasPerm()) setScreen("home") else showPermission()
    }

    override fun onResume() {
        super.onResume()
        if (screen == "permission" && hasPerm()) setScreen("home")
    }

    override fun onBackPressed() {
        if (screen == "settings" || screen == "compress_image") {
            setScreen("home")
        } else {
            super.onBackPressed()
        }
    }

    private fun setScreen(s: String) {
        when (s) {
            "settings" -> {
                activeTab = "settings"
                screen = "settings"
                showSettings()
            }
            "home" -> {
                activeTab = "home"
                screen = "home"
                showHome()
            }
            "compress_image" -> {
                screen = "compress_image"
                showCompressionSettings()
            }
            else -> {
                activeTab = "home"
                screen = "home"
                showHome()
            }
        }
    }

    private fun choose(title: String, options: Array<String>, current: Int, onSelect: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, current) { dialog, which ->
                onSelect(which)
                dialog.dismiss()
            }
            .setNegativeButton(tr("Cancel"), null)
            .show()
    }

    private fun showThemeDialog() {
        choose(tr("App Theme"), arrayOf(tr("Light"), tr("Dark")), if (darkMode) 1 else 0) { which ->
            darkMode = which == 1
            prefs.edit().putBoolean("dark_mode", darkMode).apply()
            applyStatusBarStyle()
            setScreen(screen)
        }
    }

    private fun logoTile(size: Int): FrameLayout {
        val box = FrameLayout(this)
        box.background = grad(intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()), dpf(size / 3))
        val t = tv("C", size * 0.55f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        box.addView(t, FrameLayout.LayoutParams(MATCH, MATCH))
        return box
    }

    private fun brandRow(size: Float): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(logoTile(dp(30)), lp(dp(30), dp(30)))
        val name = gradText(tr("CompressPro"), size, intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()))
        row.addView(name, lpm(WRAP, WRAP, 10, 0, 0, 0))
        return row
    }

    private fun tagline(size: Float): TextView {
        return tv(tr("Compress images and PDFs instantly"), size, colTextSub())
    }

    private fun navItem(label: String, glyph: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL
        item.setPadding(dp(10), dp(8), dp(10), dp(8))
        item.isClickable = true
        item.setOnClickListener { onClick() }
        val color = if (active) colAccent() else colNavInactive()
        val icon = tv(glyph, 16f, color, true)
        icon.gravity = Gravity.CENTER
        item.addView(icon, lp(dp(24), dp(24)))
        val t = tv(label, 11f, color, active)
        t.gravity = Gravity.CENTER_HORIZONTAL
        val tlp = lp(WRAP, WRAP)
        tlp.topMargin = dp(4)
        item.addView(t, tlp)
        return item
    }

    private fun navBar(): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setBackgroundColor(colNavBg())
        bar.setPadding(dp(10), dp(8), dp(10), dp(8))
        val home = navItem(tr("Home"), "H", activeTab == "home") { setScreen("home") }
        val settings = navItem(tr("Settings"), "S", activeTab == "settings") { setScreen("settings") }
        bar.addView(home, lp(0, WRAP).also { it.weight = 1f })
        bar.addView(settings, lp(0, WRAP).also { it.weight = 1f })
        return bar
    }

    private fun badge(text: String): TextView {
        val b = tv(text, 10f, Color.WHITE, true)
        b.setPadding(dp(8), dp(3), dp(8), dp(3))
        b.background = rr(0x66000000.toInt(), dpf(10))
        return b
    }

    private fun badgeLp(): FrameLayout.LayoutParams {
        val p = FrameLayout.LayoutParams(WRAP, WRAP)
        p.gravity = Gravity.TOP or Gravity.END
        p.topMargin = dp(14)
        p.rightMargin = dp(14)
        return p
    }

    private fun bigCard(
        colors: IntArray,
        glyph: String,
        title: String,
        subtitle: String,
        desc: String,
        accent: Int,
        soon: Boolean,
        onClick: () -> Unit = {}
    ): FrameLayout {
        val card = FrameLayout(this)
        card.background = grad(colors, dpf(22))
        card.isClickable = true
        card.setOnClickListener { onClick() }
        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.VERTICAL
        inner.setPadding(dp(18), dp(18), dp(18), dp(18))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL

        val iconBox = FrameLayout(this)
        iconBox.background = rr(accent, dpf(14))
        val iconT = tv(glyph, 20f, Color.WHITE, true)
        iconT.gravity = Gravity.CENTER
        iconBox.addView(iconT, FrameLayout.LayoutParams(dp(44), dp(44)))
        top.addView(iconBox, lp(dp(44), dp(44)))

        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        val titleT = tv(title, 19f, Color.WHITE, true)
        val subT = tv(subtitle, 15f, 0xDDFFFFFF.toInt())
        titleBox.addView(titleT)
        titleBox.addView(subT)
        top.addView(titleBox, lpm(WRAP, WRAP, 12, 0, 0, 0))

        inner.addView(top, lp(MATCH, WRAP))

        val descT = tv(desc, 12f, 0xEEFFFFFF.toInt())
        inner.addView(descT, lpm(MATCH, WRAP, 0, 14, 0, 0))

        if (soon) {
            card.addView(badge(tr("Coming Soon")), badgeLp())
        }

        card.addView(inner, FrameLayout.LayoutParams(MATCH, WRAP))
        return card
    }

    private fun showHome() {
        root.removeAllViews()
        val outer = FrameLayout(this)
        outer.setBackgroundColor(colBg())

        val mainCol = LinearLayout(this)
        mainCol.orientation = LinearLayout.VERTICAL

        val frame = FrameLayout(this)

        if (darkMode) {
            val wave = WaveView(this)
            frame.addView(wave, FrameLayout.LayoutParams(MATCH, dp(130), Gravity.BOTTOM))
        }

        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(20), dp(16), dp(20), dp(20))

        c.addView(brandRow(20f), lpm(WRAP, WRAP, 0, 0, 0, 6))
        c.addView(tagline(11f), lpm(WRAP, WRAP, 0, 0, 0, 20))

        val headline = tv(tr("Make Your Files"), 20f, colText(), true)
        c.addView(headline, lpm(WRAP, WRAP, 0, 0, 0, 2))

        val headline2 = gradText(tr("Smaller. Lighter."), 20f, intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()))
        c.addView(headline2, lpm(WRAP, WRAP, 0, 0, 0, 10))

        val descT = tv(tr("Reduce file size instantly without losing quality. Fast, simple and free."), 12f, colTextSub())
        c.addView(descT, lpm(MATCH, WRAP, 0, 0, 0, 24))

        val imageCard = bigCard(
            intArrayOf(0xFF3D9BFF.toInt(), 0xFF3D6EFF.toInt(), 0xFF8A2BFF.toInt()),
            "I", tr("Image"), tr("Compress / Resize"),
            tr("Shrink photos while keeping them sharp and clear."),
            0xFF3D6EFF.toInt(), false
        ) { setScreen("compress_image") }
        c.addView(imageCard, lp(MATCH, WRAP))

        val pdfCard = bigCard(
            intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()),
            "P", tr("PDF"), tr("Compress / Resize"),
            tr("Make your PDF files smaller and fit your needs."),
            0xFFFF3D6E.toInt(), true
        ) { }
        c.addView(pdfCard, lpm(MATCH, WRAP, 0, 14, 0, 0))

        scroll.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))

        mainCol.addView(frame, lp(MATCH, 0).also { it.weight = 1f })
        mainCol.addView(navBar(), lp(MATCH, WRAP))

        outer.addView(mainCol, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(outer, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun cardBox(): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = rr(colCard(), dpf(18))
        box.setPadding(dp(4), dp(4), dp(4), dp(4))
        return box
    }

    private fun row(title: String, value: String, last: Boolean, onClick: () -> Unit): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL

        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.HORIZONTAL
        inner.gravity = Gravity.CENTER_VERTICAL
        inner.setPadding(dp(14), dp(14), dp(14), dp(14))
        inner.isClickable = true
        inner.setOnClickListener { onClick() }

        val t = tv(title, 15f, colText())
        val tlp = lp(0, WRAP)
        tlp.weight = 1f
        inner.addView(t, tlp)

        if (value.isNotEmpty()) {
            val v = tv(value, 12f, colTextSub())
            inner.addView(v, lp(WRAP, WRAP))
        }

        r.addView(inner, lp(MATCH, WRAP))

        if (!last) {
            val div = FrameLayout(this)
            div.setBackgroundColor(colDivider())
            r.addView(div, lpm(MATCH, 1, 14, 0, 14, 0))
        }

        return r
    }

    private fun showSettings() {
        root.removeAllViews()
        val outer = FrameLayout(this)
        outer.setBackgroundColor(colBg())

        val mainCol = LinearLayout(this)
        mainCol.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(this)
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(20), dp(16), dp(20), dp(20))

        c.addView(brandRow(18f), lpm(WRAP, WRAP, 0, 0, 0, 16))

        val title = tv(tr("Settings"), 22f, colText(), true)
        c.addView(title, lpm(WRAP, WRAP, 0, 0, 0, 18))

        val card1 = cardBox()
        card1.addView(row(tr("App Theme"), if (darkMode) tr("Dark") else tr("Light"), false) { showThemeDialog() })
        card1.addView(row(tr("Language"), tr("English"), false) { })
        card1.addView(row(tr("Output Quality"), tr("High"), false) { })
        card1.addView(row(tr("Storage Location"), tr("Internal Storage"), false) { })
        card1.addView(row(tr("About App"), tr("Version 1.0"), true) { })
        c.addView(card1, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val card2 = cardBox()
        card2.addView(row(tr("Privacy Policy"), "", false) { })
        card2.addView(row(tr("Rate Us"), "", true) { })
        c.addView(card2, lp(MATCH, WRAP))

        scroll.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))
        mainCol.addView(scroll, lp(MATCH, 0).also { it.weight = 1f })
        mainCol.addView(navBar(), lp(MATCH, WRAP))

        outer.addView(mainCol, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(outer, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun sectionCard(): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = rr(0xFF131B2E.toInt(), dpf(18))
        card.setPadding(dp(16), dp(16), dp(16), dp(16))
        return card
    }

    private fun sectionHeader(
        iconGlyph: String,
        iconColors: IntArray,
        title: String,
        subtitle: String
    ): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val iconBox = FrameLayout(this)
        iconBox.background = grad(iconColors, dpf(12))
        val iconT = tv(iconGlyph, 14f, Color.WHITE, true)
        iconT.gravity = Gravity.CENTER
        iconBox.addView(iconT, FrameLayout.LayoutParams(dp(40), dp(40)))
        row.addView(iconBox, lp(dp(40), dp(40)))

        val textBox = LinearLayout(this)
        textBox.orientation = LinearLayout.VERTICAL
        val t = tv(title, 16f, Color.WHITE, true)
        val s = tv(subtitle, 11f, 0xFF9AA4B8.toInt())
        textBox.addView(t)
        textBox.addView(s)
        val tlp = lp(0, WRAP)
        tlp.weight = 1f
        tlp.marginStart = dp(12)
        row.addView(textBox, tlp)

        return row
    }

    private fun pillButton(text: String, selected: Boolean, onClick: () -> Unit): TextView {
        val b = tv(text, 12f, if (selected) Color.WHITE else 0xFFB0B8C9.toInt(), selected)
        b.gravity = Gravity.CENTER
        b.setPadding(dp(10), dp(12), dp(10), dp(12))
        if (selected) {
            b.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(14))
        } else {
            b.background = rr(0xFF0F1A2E.toInt(), dpf(14))
        }
        b.isClickable = true
        b.setOnClickListener { onClick() }
        return b
    }

    private fun thumbnail(index: Int): FrameLayout {
        val box = FrameLayout(this)
        val bgColor = 0xFF1C2538.toInt()
        if (index == selectedThumbnail) {
            val d = GradientDrawable()
            d.setColor(bgColor)
            d.cornerRadius = dpf(14)
            d.setStroke(dp(2), 0xFF2E8BFF.toInt())
            box.background = d
        } else {
            box.background = rr(bgColor, dpf(14))
        }
        val label = tv("IMG", 11f, 0xFF6B7689.toInt(), true)
        label.gravity = Gravity.CENTER
        box.addView(label, FrameLayout.LayoutParams(MATCH, MATCH))
        if (index == selectedThumbnail) {
            val check = tv("OK", 9f, Color.WHITE, true)
            check.gravity = Gravity.CENTER
            check.background = rr(0xFF2E8BFF.toInt(), dpf(20))
            val clp = FrameLayout.LayoutParams(dp(22), dp(22))
            clp.gravity = Gravity.BOTTOM or Gravity.END
            clp.rightMargin = dp(6)
            clp.bottomMargin = dp(6)
            box.addView(check, clp)
        }
        box.isClickable = true
        box.setOnClickListener {
            selectedThumbnail = index
            showCompressionSettings()
        }
        return box
    }

    private fun showCompressionSettings() {
        root.removeAllViews()
        val outer = FrameLayout(this)
        outer.setBackgroundColor(0xFF0B1220.toInt())

        val mainCol = LinearLayout(this)
        mainCol.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(this)
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(20), dp(20), dp(20), dp(20))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val backBtn = FrameLayout(this)
        backBtn.background = rr(0xFF1C2538.toInt(), dpf(20))
        val backArrow = tv("<", 18f, Color.WHITE, true)
        backArrow.gravity = Gravity.CENTER
        backBtn.addView(backArrow, FrameLayout.LayoutParams(dp(40), dp(40)))
        backBtn.isClickable = true
        backBtn.setOnClickListener { setScreen("home") }
        header.addView(backBtn, lp(dp(40), dp(40)))

        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        val titleT = tv(tr("Compression Settings"), 18f, Color.WHITE, true)
        val subT = tv(tr("Adjust settings to get the best balance between quality and file size."), 11f, 0xFF9AA4B8.toInt())
        titleBox.addView(titleT)
        titleBox.addView(subT, lpm(MATCH, WRAP, 0, 2, 0, 0))
        val titleLp = lp(0, WRAP)
        titleLp.weight = 1f
        titleLp.marginStart = dp(12)
        titleLp.marginEnd = dp(10)
        header.addView(titleBox, titleLp)

        val iconTile = FrameLayout(this)
        iconTile.background = grad(intArrayOf(0xFF3D9BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(14))
        val iconGlyph = tv("IMG", 10f, Color.WHITE, true)
        iconGlyph.gravity = Gravity.CENTER
        iconTile.addView(iconGlyph, FrameLayout.LayoutParams(dp(48), dp(48)))
        header.addView(iconTile, lp(dp(48), dp(48)))

        c.addView(header, lpm(MATCH, WRAP, 0, 0, 0, 18))

        val thumbScroll = HorizontalScrollView(this)
        thumbScroll.isHorizontalScrollBarEnabled = false
        val thumbRow = LinearLayout(this)
        thumbRow.orientation = LinearLayout.HORIZONTAL
        for (i in 0 until 6) {
            val t = thumbnail(i)
            thumbRow.addView(t, lpm(dp(72), dp(72), if (i == 0) 0 else 10, 0, 0, 0))
        }
        thumbScroll.addView(thumbRow, FrameLayout.LayoutParams(WRAP, WRAP))
        c.addView(thumbScroll, lpm(MATCH, WRAP, 0, 0, 0, 18))

        val sizeCard = sectionCard()
        val sizeTop = LinearLayout(this)
        sizeTop.orientation = LinearLayout.HORIZONTAL
        sizeTop.gravity = Gravity.CENTER_VERTICAL
        val sizeLabel = tv(tr("ESTIMATED SIZE"), 11f, 0xFF7C8699.toInt(), true)
        val sizeLp = lp(0, WRAP)
        sizeLp.weight = 1f
        sizeTop.addView(sizeLabel, sizeLp)
        val savedBadge = tv(tr("-87% Saved"), 12f, Color.WHITE, true)
        savedBadge.setPadding(dp(10), dp(6), dp(10), dp(6))
        savedBadge.background = rr(0xFF1E7A45.toInt(), dpf(14))
        sizeTop.addView(savedBadge, lp(WRAP, WRAP))
        sizeCard.addView(sizeTop, lpm(MATCH, WRAP, 0, 0, 0, 8))

        val sizeRow = LinearLayout(this)
        sizeRow.orientation = LinearLayout.HORIZONTAL
        sizeRow.gravity = Gravity.BOTTOM
        val bigSize = tv("1.2", 26f, Color.WHITE, true)
        sizeRow.addView(bigSize)
        val unit = tv(" MB", 16f, 0xFF9AA4B8.toInt(), true)
        sizeRow.addView(unit)
        sizeCard.addView(sizeRow)

        val afterText = tv(tr("(After Compression)"), 11f, 0xFF7C8699.toInt())
        sizeCard.addView(afterText, lpm(WRAP, WRAP, 0, 2, 0, 10))

        val track = FrameLayout(this)
        track.background = rr(0xFF0F1A2E.toInt(), dpf(6))
        val fill = FrameLayout(this)
        fill.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(6))
        track.addView(fill, FrameLayout.LayoutParams(dp(170), dp(8)))
        sizeCard.addView(track, lpm(MATCH, dp(8), 0, 0, 0, 10))

        val origRow = tv(tr("Original: 9.8 MB"), 11f, 0xFF7C8699.toInt())
        origRow.gravity = Gravity.END
        sizeCard.addView(origRow, lp(MATCH, WRAP))

        c.addView(sizeCard, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val qualityCard = sectionCard()
        val qHeader = sectionHeader("Q", intArrayOf(0xFF8A2BFF.toInt(), 0xFF3D6EFF.toInt()), tr("Quality"), tr("Adjust the compression quality."))
        val qBadge = tv(qualityPercent.toString() + "%", 12f, Color.WHITE, true)
        qBadge.setPadding(dp(12), dp(6), dp(12), dp(6))
        qBadge.background = rr(0xFF3D2E7A.toInt(), dpf(14))
        qHeader.addView(qBadge, lp(WRAP, WRAP))
        qualityCard.addView(qHeader, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val seek = SeekBar(this)
        seek.max = 100
        seek.progress = qualityPercent
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
                qualityPercent = value
                qBadge.text = value.toString() + "%"
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
            }
        })
        qualityCard.addView(seek, lpm(MATCH, WRAP, 0, 0, 0, 10))

        val qRow = LinearLayout(this)
        qRow.orientation = LinearLayout.HORIZONTAL
        val lowBtn = pillButton(tr("Low (30%)"), qualityPercent == 30) { qualityPercent = 30; showCompressionSettings() }
        val medBtn = pillButton(tr("Medium (60%)"), qualityPercent == 60) { qualityPercent = 60; showCompressionSettings() }
        val highBtn = pillButton(tr("High (90%)"), qualityPercent == 90) { qualityPercent = 90; showCompressionSettings() }
        val lowLp = lp(0, WRAP)
        lowLp.weight = 1f
        lowLp.marginEnd = dp(8)
        qRow.addView(lowBtn, lowLp)
        val medLp = lp(0, WRAP)
        medLp.weight = 1f
        medLp.marginEnd = dp(8)
        qRow.addView(medBtn, medLp)
        val highLp = lp(0, WRAP)
        highLp.weight = 1f
        qRow.addView(highBtn, highLp)
        qualityCard.addView(qRow, lp(MATCH, WRAP))

        c.addView(qualityCard, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val dimCard = sectionCard()
        val dHeader = sectionHeader("D", intArrayOf(0xFF1FD1C7.toInt(), 0xFF2E8BFF.toInt()), tr("Dimensions"), tr("Set your desired width and height."))
        val dBadge = tv("80%", 12f, Color.WHITE, true)
        dBadge.setPadding(dp(12), dp(6), dp(12), dp(6))
        dBadge.background = rr(0xFF123A5C.toInt(), dpf(14))
        dHeader.addView(dBadge, lp(WRAP, WRAP))
        dimCard.addView(dHeader, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val dimRow = LinearLayout(this)
        dimRow.orientation = LinearLayout.HORIZONTAL
        dimRow.gravity = Gravity.CENTER_VERTICAL

        val heightBox = LinearLayout(this)
        heightBox.orientation = LinearLayout.VERTICAL
        heightBox.addView(tv(tr("Height"), 12f, 0xFF9AA4B8.toInt()))
        val heightInput = EditText(this)
        heightInput.setText(heightPx)
        heightInput.inputType = InputType.TYPE_CLASS_NUMBER
        heightInput.setTextColor(Color.WHITE)
        heightInput.textSize = 14f
        heightInput.background = rr(0xFF0F1A2E.toInt(), dpf(10))
        heightInput.setPadding(dp(12), dp(10), dp(12), dp(10))
        heightBox.addView(heightInput, lpm(MATCH, WRAP, 0, 4, 0, 0))
        val heightLp = lp(0, WRAP)
        heightLp.weight = 1f
        dimRow.addView(heightBox, heightLp)

        val xLabel = tv("x", 14f, 0xFF7C8699.toInt(), true)
        dimRow.addView(xLabel, lpm(WRAP, WRAP, 8, 18, 8, 0))

        val widthBox = LinearLayout(this)
        widthBox.orientation = LinearLayout.VERTICAL
        widthBox.addView(tv(tr("Width"), 12f, 0xFF9AA4B8.toInt()))
        val widthInput = EditText(this)
        widthInput.setText(widthPx)
        widthInput.inputType = InputType.TYPE_CLASS_NUMBER
        widthInput.setTextColor(Color.WHITE)
        widthInput.textSize = 14f
        widthInput.background = rr(0xFF0F1A2E.toInt(), dpf(10))
        widthInput.setPadding(dp(12), dp(10), dp(12), dp(10))
        widthBox.addView(widthInput, lpm(MATCH, WRAP, 0, 4, 0, 0))
        val widthLp = lp(0, WRAP)
        widthLp.weight = 1f
        dimRow.addView(widthBox, widthLp)

        dimCard.addView(dimRow, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val lockRow = LinearLayout(this)
        lockRow.orientation = LinearLayout.HORIZONTAL
        lockRow.gravity = Gravity.CENTER_VERTICAL
        val lockLabel = tv(tr("Lock Aspect Ratio"), 13f, 0xFFD0D5DF.toInt())
        val lockLp = lp(0, WRAP)
        lockLp.weight = 1f
        lockRow.addView(lockLabel, lockLp)
        val lockSwitch = Switch(this)
        lockSwitch.isChecked = lockAspect
        lockSwitch.setOnCheckedChangeListener { _, checked -> lockAspect = checked }
        lockRow.addView(lockSwitch, lp(WRAP, WRAP))
        dimCard.addView(lockRow, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val presetRow = LinearLayout(this)
        presetRow.orientation = LinearLayout.HORIZONTAL
        val p25 = pillButton("25%", false) { }
        val p50 = pillButton("50%", false) { }
        val p75 = pillButton("75%", false) { }
        val pCustom = pillButton(tr("Custom"), true) { }
        val p25Lp = lp(0, WRAP)
        p25Lp.weight = 1f
        p25Lp.marginEnd = dp(6)
        presetRow.addView(p25, p25Lp)
        val p50Lp = lp(0, WRAP)
        p50Lp.weight = 1f
        p50Lp.marginEnd = dp(6)
        presetRow.addView(p50, p50Lp)
        val p75Lp = lp(0, WRAP)
        p75Lp.weight = 1f
        p75Lp.marginEnd = dp(6)
        presetRow.addView(p75, p75Lp)
        val pCustomLp = lp(0, WRAP)
        pCustomLp.weight = 1f
        presetRow.addView(pCustom, pCustomLp)
        dimCard.addView(presetRow, lp(MATCH, WRAP))

        c.addView(dimCard, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val formatCard = sectionCard()
        val fHeader = sectionHeader("F", intArrayOf(0xFF8A2BFF.toInt(), 0xFFFF3D6E.toInt()), tr("Format"), tr("Choose output format for your image."))
        formatCard.addView(fHeader, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val formatRow = LinearLayout(this)
        formatRow.orientation = LinearLayout.HORIZONTAL
        val formats = arrayOf("JPG", "PNG", "WEBP", tr("Original"))
        for (i in formats.indices) {
            val f = formats[i]
            val btn = pillButton(f, selectedFormat == f) { selectedFormat = f; showCompressionSettings() }
            val blp = lp(0, WRAP)
            blp.weight = 1f
            if (i != formats.size - 1) blp.marginEnd = dp(8)
            formatRow.addView(btn, blp)
        }
        formatCard.addView(formatRow, lp(MATCH, WRAP))

        c.addView(formatCard, lpm(MATCH, WRAP, 0, 0, 0, 20))

        val compressBtn = LinearLayout(this)
        compressBtn.orientation = LinearLayout.VERTICAL
        compressBtn.gravity = Gravity.CENTER
        compressBtn.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(26))
        compressBtn.setPadding(dp(20), dp(16), dp(20), dp(16))
        val cTitle = tv(tr("Compress Image"), 16f, Color.WHITE, true)
        cTitle.gravity = Gravity.CENTER
        compressBtn.addView(cTitle)
        val cSub = tv(tr("Tap to start compression"), 11f, 0xDDFFFFFF.toInt())
        cSub.gravity = Gravity.CENTER
        compressBtn.addView(cSub, lpm(WRAP, WRAP, 0, 2, 0, 0))
        compressBtn.isClickable = true
        compressBtn.setOnClickListener { }
        c.addView(compressBtn, lpm(MATCH, WRAP, 0, 0, 0, 16))

        scroll.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))
        mainCol.addView(scroll, lp(MATCH, 0).also { it.weight = 1f })
        mainCol.addView(navBar(), lp(MATCH, WRAP))

        outer.addView(mainCol, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(outer, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun showPermission() {
        screen = "permission"
        root.removeAllViews()
        val bg = FrameLayout(this)
        bg.setBackgroundColor(colBg())

        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        c.setPadding(dp(30), dp(30), dp(30), dp(30))

        val box = FrameLayout(this)
        box.background = grad(intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt()), dpf(24))
        val icon = tv("CP", 28f, Color.WHITE, true)
        icon.gravity = Gravity.CENTER
        box.addView(icon, FrameLayout.LayoutParams(dp(90), dp(90)))
        c.addView(box, lpm(WRAP, WRAP, 0, 0, 0, 24))

        val title = tv(tr("Storage Permission Needed"), 20f, colText(), true)
        title.gravity = Gravity.CENTER
        c.addView(title, lpm(WRAP, WRAP, 0, 0, 0, 10))

        val desc = tv(tr("CompressPro needs access to your files to compress images and PDFs."), 13f, colTextSub())
        desc.gravity = Gravity.CENTER
        c.addView(desc, lpm(MATCH, WRAP, 0, 0, 0, 28))

        val btn = tv(tr("Grant Permission"), 16f, Color.WHITE, true)
        btn.gravity = Gravity.CENTER
        btn.background = grad(intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt()), dpf(26))
        btn.setPadding(dp(30), dp(14), dp(30), dp(14))
        btn.setOnClickListener {
            if (permBlocked()) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            } else {
                permissionLauncher.launch(permList())
            }
        }
        c.addView(btn, lp(WRAP, WRAP))

        bg.addView(c, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(bg, FrameLayout.LayoutParams(MATCH, MATCH))
    }
}
