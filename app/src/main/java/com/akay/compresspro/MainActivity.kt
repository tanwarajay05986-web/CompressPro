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
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var screen = "home"
    private lateinit var prefs: SharedPreferences
    private var darkMode = false
    private lateinit var root: FrameLayout

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
        if (screen == "settings") {
            setScreen("home")
        } else {
            super.onBackPressed()
        }
    }

    private fun setScreen(s: String) {
        screen = s
        if (s == "settings") showSettings() else showHome()
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
        val home = navItem(tr("Home"), "H", screen == "home") { setScreen("home") }
        val settings = navItem(tr("Settings"), "S", screen == "settings") { setScreen("settings") }
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
        soon: Boolean
    ): FrameLayout {
        val card = FrameLayout(this)
        card.background = grad(colors, dpf(22))
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
            0xFF3D6EFF.toInt(), true
        )
        c.addView(imageCard, lp(MATCH, WRAP))

        val pdfCard = bigCard(
            intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()),
            "P", tr("PDF"), tr("Compress / Resize"),
            tr("Make your PDF files smaller and fit your needs."),
            0xFFFF3D6E.toInt(), true
        )
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
