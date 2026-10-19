package com.akay.compresspro

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream

private class CircularProgressView(context: android.content.Context) : View(context) {
    var progress: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val density = context.resources.displayMetrics.density
    private val strokeW = 10f * density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        bgPaint.style = Paint.Style.STROKE
        bgPaint.strokeWidth = strokeW
        bgPaint.color = 0xFF1C2538.toInt()
        fgPaint.style = Paint.Style.STROKE
        fgPaint.strokeWidth = strokeW
        fgPaint.strokeCap = Paint.Cap.ROUND
        fgPaint.color = 0xFF2E8BFF.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = RectF(strokeW / 2, strokeW / 2, width - strokeW / 2, height - strokeW / 2)
        canvas.drawArc(rect, 0f, 360f, false, bgPaint)
        val sweep = 360f * (progress / 100f)
        canvas.drawArc(rect, -90f, sweep, false, fgPaint)
    }
}

class MainActivity : ComponentActivity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var screen = "home"
    private var activeTab = "home"
    private lateinit var prefs: SharedPreferences
    private var darkMode = false
    private lateinit var root: FrameLayout

    private var selectedImageUri: Uri? = null
    private var originalFileSize: Long = 0L
    private var originalBitmapW: Int = 0
    private var originalBitmapH: Int = 0

    private var qualityPercent = 60
    private var selectedFormat = "JPG"
    private var lockAspect = true
    private var heightPx = "720"
    private var widthPx = "1280"
    private var dimensionPreset = "Custom"

    private var compressedFile: File? = null
    private var compressedFileSize: Long = 0L

    private var progressPercent = 0
    private var stageIndex = 0
    private var isCancelled = false
    private var compressionThread: Thread? = null

    private var estimateValueRef: TextView? = null
    private var estimateSavedRef: TextView? = null
    private var estimateFillViewRef: View? = null

    private var progressRingRef: CircularProgressView? = null
    private var progressTextRef: TextView? = null
    private var timeTextRef: TextView? = null
    private var speedTextRef: TextView? = null
    private var compSizeTextRef: TextView? = null
    private val stageStatusRefs: Array<TextView?> = arrayOfNulls(3)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted() }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                onImagePicked(uri)
            }
        }

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
        when (screen) {
            "settings" -> setScreen("home")
            "compress_image" -> setScreen("home")
            "processing" -> cancelCompression()
            "result" -> setScreen("home")
            else -> super.onBackPressed()
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
            "processing" -> {
                screen = "processing"
                showProcessingScreen()
            }
            "result" -> {
                screen = "result"
                showResultScreen()
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
        onClick: () -> Unit
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
        ) { pickImageLauncher.launch("image/*") }
        c.addView(imageCard, lp(MATCH, WRAP))

        val pdfCard = bigCard(
            intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt()),
            "P", tr("PDF"), tr("Compress / Resize"),
            tr("Make your PDF files smaller and fit your needs."),
            0xFFFF3D6E.toInt(), true
        ) { Toast.makeText(this, tr("PDF compression coming soon"), Toast.LENGTH_SHORT).show() }
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

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst()) {
                    size = cursor.getLong(idx)
                }
            }
        } catch (e: Exception) {
        }
        if (size <= 0L) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    size = stream.available().toLong()
                }
            } catch (e: Exception) {
            }
        }
        return size
    }

    private fun getFileNameFromUri(uri: Uri?): String {
        if (uri == null) return ""
        var name = "image.jpg"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(idx)
                }
            }
        } catch (e: Exception) {
        }
        return name
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            String.format("%.0f KB", kb)
        }
    }

    private fun estimateCompressedSize(): Long {
        if (originalFileSize <= 0L) return 0L
        val w = widthPx.toIntOrNull() ?: originalBitmapW
        val h = heightPx.toIntOrNull() ?: originalBitmapH
        val origArea = (originalBitmapW * originalBitmapH).coerceAtLeast(1)
        val newArea = (w * h).coerceAtLeast(1)
        val areaRatio = newArea.toFloat() / origArea.toFloat()
        val qualityRatio = qualityPercent / 100f
        val formatFactor = when (selectedFormat) {
            "PNG" -> 1.4f
            "WEBP" -> 0.75f
            "Original" -> 1.0f
            else -> 0.9f
        }
        var estimate = originalFileSize * areaRatio * qualityRatio * formatFactor
        if (estimate < originalFileSize * 0.03) estimate = originalFileSize * 0.03
        if (estimate > originalFileSize.toDouble()) estimate = originalFileSize.toDouble()
        return estimate.toLong()
    }

    private fun updateEstimateDisplay() {
        val est = estimateCompressedSize()
        estimateValueRef?.text = formatSize(est)
        val pct = if (originalFileSize > 0) (100 - (est * 100 / originalFileSize)).toInt().coerceIn(1, 99) else 50
        estimateSavedRef?.text = "-" + pct.toString() + "% " + tr("Saved")
        val fv = estimateFillViewRef
        if (fv != null) {
            val lp2 = fv.layoutParams
            lp2.width = dp((240 * pct) / 100)
            fv.layoutParams = lp2
        }
    }

    private fun setDimensionPercent(ratio: Double, label: String) {
        if (originalBitmapW > 0 && originalBitmapH > 0) {
            widthPx = (originalBitmapW * ratio).toInt().coerceAtLeast(1).toString()
            heightPx = (originalBitmapH * ratio).toInt().coerceAtLeast(1).toString()
        }
        dimensionPreset = label
    }

    private fun onImagePicked(uri: Uri) {
        selectedImageUri = uri
        originalFileSize = getFileSize(uri)
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        try {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
        }
        originalBitmapW = if (opts.outWidth > 0) opts.outWidth else 1280
        originalBitmapH = if (opts.outHeight > 0) opts.outHeight else 720
        widthPx = originalBitmapW.toString()
        heightPx = originalBitmapH.toString()
        qualityPercent = 60
        selectedFormat = "JPG"
        lockAspect = true
        dimensionPreset = "Custom"
        compressedFile = null
        setScreen("compress_image")
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
        header.addView(titleBox, titleLp)

        c.addView(header, lpm(MATCH, WRAP, 0, 0, 0, 18))

        val previewCard = FrameLayout(this)
        previewCard.background = rr(0xFF131B2E.toInt(), dpf(16))
        val previewImg = ImageView(this)
        previewImg.scaleType = ImageView.ScaleType.CENTER_CROP
        selectedImageUri?.let { previewImg.setImageURI(it) }
        val pad = dp(6)
        previewImg.setPadding(pad, pad, pad, pad)
        previewCard.addView(previewImg, FrameLayout.LayoutParams(MATCH, dp(180)))
        c.addView(previewCard, lpm(MATCH, dp(180), 0, 0, 0, 10))

        val changeBtn = tv(tr("Change Image"), 13f, 0xFF2E8BFF.toInt(), true)
        changeBtn.gravity = Gravity.CENTER
        changeBtn.setPadding(dp(10), dp(10), dp(10), dp(10))
        changeBtn.isClickable = true
        changeBtn.setOnClickListener { pickImageLauncher.launch("image/*") }
        c.addView(changeBtn, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val sizeCard = sectionCard()
        val sizeTop = LinearLayout(this)
        sizeTop.orientation = LinearLayout.HORIZONTAL
        sizeTop.gravity = Gravity.CENTER_VERTICAL
        val sizeLabel = tv(tr("ESTIMATED SIZE"), 11f, 0xFF7C8699.toInt(), true)
        val sizeLabelLp = lp(0, WRAP)
        sizeLabelLp.weight = 1f
        sizeTop.addView(sizeLabel, sizeLabelLp)
        val savedBadge = tv("", 12f, Color.WHITE, true)
        savedBadge.setPadding(dp(10), dp(6), dp(10), dp(6))
        savedBadge.background = rr(0xFF1E7A45.toInt(), dpf(14))
        estimateSavedRef = savedBadge
        sizeTop.addView(savedBadge, lp(WRAP, WRAP))
        sizeCard.addView(sizeTop, lpm(MATCH, WRAP, 0, 0, 0, 8))

        val sizeRow = LinearLayout(this)
        sizeRow.orientation = LinearLayout.HORIZONTAL
        sizeRow.gravity = Gravity.BOTTOM
        val bigSize = tv("", 26f, Color.WHITE, true)
        estimateValueRef = bigSize
        sizeRow.addView(bigSize)
        sizeCard.addView(sizeRow)

        val afterText = tv(tr("(After Compression)"), 11f, 0xFF7C8699.toInt())
        sizeCard.addView(afterText, lpm(WRAP, WRAP, 0, 2, 0, 10))

        val trackBg = FrameLayout(this)
        trackBg.background = rr(0xFF0F1A2E.toInt(), dpf(6))
        val fillView = View(this)
        fillView.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(6))
        val fillLp = FrameLayout.LayoutParams(dp(10), dp(8))
        trackBg.addView(fillView, fillLp)
        estimateFillViewRef = fillView
        sizeCard.addView(trackBg, lpm(MATCH, dp(8), 0, 0, 0, 10))

        val origRow = tv(tr("Original: ") + formatSize(originalFileSize), 11f, 0xFF7C8699.toInt())
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
                val v = if (value < 5) 5 else value
                qualityPercent = v
                qBadge.text = v.toString() + "%"
                updateEstimateDisplay()
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

        var heightWatcher: TextWatcher? = null
        var widthWatcher: TextWatcher? = null

        heightWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                heightPx = s.toString()
                dimensionPreset = "Custom"
                if (lockAspect && originalBitmapW > 0 && originalBitmapH > 0) {
                    val h = heightPx.toIntOrNull()
                    if (h != null && h > 0) {
                        val newW = (h * originalBitmapW) / originalBitmapH
                        widthPx = newW.toString()
                        val wWatcher = widthWatcher
                        if (wWatcher != null) widthInput.removeTextChangedListener(wWatcher)
                        widthInput.setText(widthPx)
                        widthInput.setSelection(widthInput.text.length)
                        if (wWatcher != null) widthInput.addTextChangedListener(wWatcher)
                    }
                }
                updateEstimateDisplay()
            }
        }

        widthWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                widthPx = s.toString()
                dimensionPreset = "Custom"
                if (lockAspect && originalBitmapW > 0 && originalBitmapH > 0) {
                    val w = widthPx.toIntOrNull()
                    if (w != null && w > 0) {
                        val newH = (w * originalBitmapH) / originalBitmapW
                        heightPx = newH.toString()
                        val hWatcher = heightWatcher
                        if (hWatcher != null) heightInput.removeTextChangedListener(hWatcher)
                        heightInput.setText(heightPx)
                        heightInput.setSelection(heightInput.text.length)
                        if (hWatcher != null) heightInput.addTextChangedListener(hWatcher)
                    }
                }
                updateEstimateDisplay()
            }
        }

        heightInput.addTextChangedListener(heightWatcher)
        widthInput.addTextChangedListener(widthWatcher)

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
        val p25 = pillButton("25%", dimensionPreset == "25%") { setDimensionPercent(0.25, "25%"); showCompressionSettings() }
        val p50 = pillButton("50%", dimensionPreset == "50%") { setDimensionPercent(0.5, "50%"); showCompressionSettings() }
        val p75 = pillButton("75%", dimensionPreset == "75%") { setDimensionPercent(0.75, "75%"); showCompressionSettings() }
        val pCustom = pillButton(tr("Custom"), dimensionPreset == "Custom") { setDimensionPercent(1.0, "Custom"); showCompressionSettings() }
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
        compressBtn.setOnClickListener {
            if (selectedImageUri != null) {
                startCompression()
            }
        }
        c.addView(compressBtn, lpm(MATCH, WRAP, 0, 0, 0, 16))

        scroll.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))
        mainCol.addView(scroll, lp(MATCH, 0).also { it.weight = 1f })
        mainCol.addView(navBar(), lp(MATCH, WRAP))

        outer.addView(mainCol, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(outer, FrameLayout.LayoutParams(MATCH, MATCH))

        updateEstimateDisplay()
    }

    private fun performCompression(uri: Uri): File? {
        return try {
            val opts = BitmapFactory.Options()
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888
            val input = contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input, null, opts)
            input.close()
            if (original == null) return null

            val targetW = (widthPx.toIntOrNull() ?: original.width).coerceAtLeast(1)
            val targetH = (heightPx.toIntOrNull() ?: original.height).coerceAtLeast(1)

            val scaled = if (targetW != original.width || targetH != original.height) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            val ext = when (selectedFormat) {
                "PNG" -> "png"
                "WEBP" -> "webp"
                else -> "jpg"
            }
            val compressFormat = when (selectedFormat) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }

            val outDir = File(cacheDir, "compressed")
            if (!outDir.exists()) outDir.mkdirs()
            val outFile = File(outDir, "compressed_" + System.currentTimeMillis() + "." + ext)
            val fos = FileOutputStream(outFile)
            scaled.compress(compressFormat, qualityPercent, fos)
            fos.flush()
            fos.close()

            if (scaled != original) {
                original.recycle()
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    private fun cancelCompression() {
        isCancelled = true
        setScreen("home")
    }

    private fun startCompression() {
        val uri = selectedImageUri ?: return
        isCancelled = false
        stageIndex = 0
        progressPercent = 0
        setScreen("processing")

        val handler = Handler(Looper.getMainLooper())

        compressionThread = Thread {
            try {
                handler.post { stageIndex = 0; refreshProcessingUi() }
                var p = 0
                while (p < 30 && !isCancelled) {
                    Thread.sleep(40)
                    p += 2
                    val cp = p
                    handler.post { progressPercent = cp; refreshProcessingUi() }
                }

                handler.post { stageIndex = 1; refreshProcessingUi() }

                val resultFile = performCompression(uri)

                var p2 = progressPercent
                while (p2 < 90 && !isCancelled) {
                    Thread.sleep(35)
                    p2 += 2
                    val cp = p2
                    handler.post { progressPercent = cp; refreshProcessingUi() }
                }

                handler.post { stageIndex = 2; refreshProcessingUi() }
                Thread.sleep(300)

                var p3 = progressPercent
                while (p3 < 100 && !isCancelled) {
                    Thread.sleep(20)
                    p3 += 2
                    val cp = p3
                    handler.post { progressPercent = cp; refreshProcessingUi() }
                }

                if (!isCancelled && resultFile != null) {
                    compressedFile = resultFile
                    compressedFileSize = resultFile.length()
                    handler.post {
                        stageIndex = 3
                        setScreen("result")
                    }
                } else if (!isCancelled) {
                    handler.post {
                        Toast.makeText(this, tr("Compression failed. Try again."), Toast.LENGTH_SHORT).show()
                        setScreen("compress_image")
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    Toast.makeText(this, tr("Compression failed. Try again."), Toast.LENGTH_SHORT).show()
                    setScreen("compress_image")
                }
            }
        }
        compressionThread?.start()
    }

    private fun refreshProcessingUi() {
        progressRingRef?.progress = progressPercent
        progressTextRef?.text = progressPercent.toString() + "%"
        val remaining = ((100 - progressPercent) / 8).coerceAtLeast(0)
        timeTextRef?.text = String.format("00:%02d", remaining)
        speedTextRef?.text = String.format("%.1f MB/s", 1.5 + (progressPercent % 10) / 10.0)

        val ratio = estimateCompressedSize().toDouble() / originalFileSize.toDouble().coerceAtLeast(1.0)
        val estNow = originalFileSize - ((originalFileSize - (originalFileSize * ratio)) * (progressPercent / 100.0))
        compSizeTextRef?.text = formatSize(estNow.toLong())

        for (i in 0..2) {
            val status = when {
                i < stageIndex -> tr("Done")
                i == stageIndex -> tr("In Progress")
                else -> tr("Waiting")
            }
            stageStatusRefs[i]?.text = status
            stageStatusRefs[i]?.setTextColor(
                if (i < stageIndex) 0xFF3DDC84.toInt() else if (i == stageIndex) 0xFF2E8BFF.toInt() else 0xFF7C8699.toInt()
            )
        }
    }

    private fun showProcessingScreen() {
        root.removeAllViews()
        val outer = FrameLayout(this)
        outer.setBackgroundColor(0xFF0B1220.toInt())

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
        backBtn.setOnClickListener { cancelCompression() }
        header.addView(backBtn, lp(dp(40), dp(40)))

        val titleBox = LinearLayout(this)
        titleBox.orientation = LinearLayout.VERTICAL
        val titleT = tv(tr("Compressing Image"), 18f, Color.WHITE, true)
        val subT = tv(tr("Please wait while we compress your image..."), 11f, 0xFF9AA4B8.toInt())
        titleBox.addView(titleT)
        titleBox.addView(subT, lpm(MATCH, WRAP, 0, 2, 0, 0))
        val titleLp = lp(0, WRAP)
        titleLp.weight = 1f
        titleLp.marginStart = dp(12)
        header.addView(titleBox, titleLp)

        val modeBadge = tv(if (qualityPercent >= 80) tr("High Quality Mode") else tr("Standard Mode"), 11f, Color.WHITE, true)
        modeBadge.setPadding(dp(10), dp(8), dp(10), dp(8))
        modeBadge.background = rr(0xFF3D2E7A.toInt(), dpf(14))
        header.addView(modeBadge, lp(WRAP, WRAP))

        c.addView(header, lpm(MATCH, WRAP, 0, 0, 0, 24))

        val midRow = LinearLayout(this)
        midRow.orientation = LinearLayout.HORIZONTAL
        midRow.gravity = Gravity.CENTER_VERTICAL

        val timeBox = LinearLayout(this)
        timeBox.orientation = LinearLayout.VERTICAL
        timeBox.background = rr(0xFF131B2E.toInt(), dpf(14))
        timeBox.setPadding(dp(14), dp(12), dp(14), dp(12))
        timeBox.addView(tv(tr("Estimated Time"), 11f, 0xFF7C8699.toInt()))
        val timeVal = tv("00:12", 18f, Color.WHITE, true)
        timeTextRef = timeVal
        timeBox.addView(timeVal, lpm(WRAP, WRAP, 0, 4, 0, 0))
        timeBox.addView(tv(tr("remaining"), 10f, 0xFF7C8699.toInt()))

        val ringBox = FrameLayout(this)
        val ring = CircularProgressView(this)
        progressRingRef = ring
        ringBox.addView(ring, FrameLayout.LayoutParams(dp(160), dp(160)))
        val pctBox = LinearLayout(this)
        pctBox.orientation = LinearLayout.VERTICAL
        pctBox.gravity = Gravity.CENTER
        val pctText = tv(progressPercent.toString() + "%", 22f, Color.WHITE, true)
        pctText.gravity = Gravity.CENTER
        progressTextRef = pctText
        pctBox.addView(pctText)
        ringBox.addView(pctBox, FrameLayout.LayoutParams(dp(160), dp(160)))

        val speedBox = LinearLayout(this)
        speedBox.orientation = LinearLayout.VERTICAL
        speedBox.background = rr(0xFF131B2E.toInt(), dpf(14))
        speedBox.setPadding(dp(14), dp(12), dp(14), dp(12))
        speedBox.addView(tv(tr("Processing..."), 11f, 0xFF7C8699.toInt()))
        val speedVal = tv("2.4 MB/s", 18f, Color.WHITE, true)
        speedTextRef = speedVal
        speedBox.addView(speedVal, lpm(WRAP, WRAP, 0, 4, 0, 0))

        val timeLp = lp(0, WRAP)
        timeLp.weight = 1f
        timeLp.marginEnd = dp(8)
        midRow.addView(timeBox, timeLp)
        midRow.addView(ringBox, lp(dp(160), dp(160)))
        val speedLp = lp(0, WRAP)
        speedLp.weight = 1f
        speedLp.marginStart = dp(8)
        midRow.addView(speedBox, speedLp)

        c.addView(midRow, lpm(MATCH, WRAP, 0, 0, 0, 24))

        val fileCard = sectionCard()
        val fileRow = LinearLayout(this)
        fileRow.orientation = LinearLayout.HORIZONTAL
        fileRow.gravity = Gravity.CENTER_VERTICAL

        val thumbBox = FrameLayout(this)
        thumbBox.background = rr(0xFF1C2538.toInt(), dpf(10))
        val thumbImg = ImageView(this)
        thumbImg.scaleType = ImageView.ScaleType.CENTER_CROP
        selectedImageUri?.let { thumbImg.setImageURI(it) }
        thumbBox.addView(thumbImg, FrameLayout.LayoutParams(dp(56), dp(56)))
        fileRow.addView(thumbBox, lp(dp(56), dp(56)))

        val sizesBox = LinearLayout(this)
        sizesBox.orientation = LinearLayout.HORIZONTAL

        val origBox = LinearLayout(this)
        origBox.orientation = LinearLayout.VERTICAL
        origBox.addView(tv(tr("Original Size"), 11f, 0xFF7C8699.toInt()))
        origBox.addView(tv(formatSize(originalFileSize), 15f, Color.WHITE, true), lpm(WRAP, WRAP, 0, 2, 0, 0))
        val origLp = lp(0, WRAP)
        origLp.weight = 1f
        sizesBox.addView(origBox, origLp)

        val compBox = LinearLayout(this)
        compBox.orientation = LinearLayout.VERTICAL
        compBox.addView(tv(tr("Compressed Size"), 11f, 0xFF7C8699.toInt()))
        val compVal = tv(formatSize(estimateCompressedSize()), 15f, Color.WHITE, true)
        compSizeTextRef = compVal
        compBox.addView(compVal, lpm(WRAP, WRAP, 0, 2, 0, 0))
        val compLp = lp(0, WRAP)
        compLp.weight = 1f
        sizesBox.addView(compBox, compLp)

        val sizesLp = lp(0, WRAP)
        sizesLp.weight = 1f
        sizesLp.marginStart = dp(12)
        fileRow.addView(sizesBox, sizesLp)
        fileCard.addView(fileRow, lp(MATCH, WRAP))

        val fileNameText = tv(getFileNameFromUri(selectedImageUri), 11f, 0xFF7C8699.toInt())
        fileCard.addView(fileNameText, lpm(WRAP, WRAP, 0, 10, 0, 0))

        c.addView(fileCard, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val stageCard = sectionCard()
        val stageNames = arrayOf(
            Pair(tr("Reading image"), tr("Analyzing file and preparing...")),
            Pair(tr("Compressing"), tr("Reducing file size and optimizing quality...")),
            Pair(tr("Saving file"), tr("Finalizing and saving to your device..."))
        )
        for (i in 0..2) {
            val sRow = LinearLayout(this)
            sRow.orientation = LinearLayout.HORIZONTAL
            sRow.gravity = Gravity.CENTER_VERTICAL
            sRow.setPadding(0, dp(8), 0, dp(8))

            val iconCircle = FrameLayout(this)
            iconCircle.background = rr(0xFF1C2538.toInt(), dpf(16))
            iconCircle.addView(View(this), FrameLayout.LayoutParams(dp(32), dp(32)))
            sRow.addView(iconCircle, lp(dp(32), dp(32)))

            val textBox = LinearLayout(this)
            textBox.orientation = LinearLayout.VERTICAL
            textBox.addView(tv(stageNames[i].first, 14f, Color.WHITE, true))
            textBox.addView(tv(stageNames[i].second, 11f, 0xFF7C8699.toInt()))
            val tLp = lp(0, WRAP)
            tLp.weight = 1f
            tLp.marginStart = dp(10)
            sRow.addView(textBox, tLp)

            val statusT = tv(tr("Waiting"), 11f, 0xFF7C8699.toInt(), true)
            stageStatusRefs[i] = statusT
            sRow.addView(statusT, lp(WRAP, WRAP))

            stageCard.addView(sRow, lp(MATCH, WRAP))
        }
        c.addView(stageCard, lpm(MATCH, WRAP, 0, 0, 0, 20))

        val waitBtn = LinearLayout(this)
        waitBtn.orientation = LinearLayout.VERTICAL
        waitBtn.gravity = Gravity.CENTER
        waitBtn.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(24))
        waitBtn.setPadding(dp(16), dp(14), dp(16), dp(14))
        val wTitle = tv(tr("Please Wait..."), 15f, Color.WHITE, true)
        wTitle.gravity = Gravity.CENTER
        waitBtn.addView(wTitle)
        val wSub = tv(tr("This may take a few seconds"), 11f, 0xDDFFFFFF.toInt())
        wSub.gravity = Gravity.CENTER
        waitBtn.addView(wSub, lpm(WRAP, WRAP, 0, 2, 0, 0))
        c.addView(waitBtn, lpm(MATCH, WRAP, 0, 0, 0, 12))

        val cancelBtn = tv(tr("Cancel"), 15f, 0xFFFF4D6D.toInt(), true)
        cancelBtn.gravity = Gravity.CENTER
        val cancelBorder = GradientDrawable()
        cancelBorder.setColor(Color.TRANSPARENT)
        cancelBorder.cornerRadius = dpf(24)
        cancelBorder.setStroke(dp(1), 0xFFFF4D6D.toInt())
        cancelBtn.background = cancelBorder
        cancelBtn.setPadding(dp(16), dp(14), dp(16), dp(14))
        cancelBtn.isClickable = true
        cancelBtn.setOnClickListener { cancelCompression() }
        c.addView(cancelBtn, lp(MATCH, WRAP))

        outer.addView(c, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(outer, FrameLayout.LayoutParams(MATCH, MATCH))

        refreshProcessingUi()
    }

    private fun shareCompressedFile() {
        val file = compressedFile ?: return
        try {
            val uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = contentResolver.getType(uri) ?: "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, tr("Share compressed image")))
        } catch (e: Exception) {
            Toast.makeText(this, tr("Unable to share file"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCompressedFile() {
        val file = compressedFile ?: return
        try {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + file.extension)
            if (Build.VERSION.SDK_INT >= 29) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CompressPro")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(this, tr("Saved to Pictures/CompressPro"), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, tr("Unable to save file"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, tr("Unable to save file"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResultScreen() {
        root.removeAllViews()
        val outer = FrameLayout(this)
        outer.setBackgroundColor(0xFF0B1220.toInt())

        val scroll = ScrollView(this)
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(20), dp(20), dp(20), dp(20))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        header.addView(logoTile(dp(36)), lp(dp(36), dp(36)))
        val nameBox = LinearLayout(this)
        nameBox.orientation = LinearLayout.VERTICAL
        nameBox.addView(gradText(tr("CompressPro"), 16f, intArrayOf(0xFFFF9A1F.toInt(), 0xFFFF3D6E.toInt(), 0xFF8A2BFF.toInt())))
        nameBox.addView(tv(tr("Smaller Size - Same Quality"), 10f, 0xFF9AA4B8.toInt()))
        val nameLp = lp(0, WRAP)
        nameLp.weight = 1f
        nameLp.marginStart = dp(10)
        header.addView(nameBox, nameLp)

        val homeBtn = FrameLayout(this)
        homeBtn.background = rr(0xFF1C2538.toInt(), dpf(12))
        val homeIcon = tv("H", 14f, Color.WHITE, true)
        homeIcon.gravity = Gravity.CENTER
        homeBtn.addView(homeIcon, FrameLayout.LayoutParams(dp(40), dp(40)))
        homeBtn.isClickable = true
        homeBtn.setOnClickListener { setScreen("home") }
        header.addView(homeBtn, lp(dp(40), dp(40)))

        c.addView(header, lpm(MATCH, WRAP, 0, 0, 0, 20))

        val checkWrap = LinearLayout(this)
        checkWrap.orientation = LinearLayout.VERTICAL
        checkWrap.gravity = Gravity.CENTER
        val checkBox = FrameLayout(this)
        val checkBg = GradientDrawable()
        checkBg.shape = GradientDrawable.OVAL
        checkBg.setColor(0xFF1E7A45.toInt())
        checkBox.background = checkBg
        val checkT = tv(tr("OK"), 20f, Color.WHITE, true)
        checkT.gravity = Gravity.CENTER
        checkBox.addView(checkT, FrameLayout.LayoutParams(dp(70), dp(70)))
        checkWrap.addView(checkBox, lp(dp(70), dp(70)))
        c.addView(checkWrap, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val doneTitle = tv(tr("Compression Completed!"), 20f, Color.WHITE, true)
        doneTitle.gravity = Gravity.CENTER
        c.addView(doneTitle, lpm(MATCH, WRAP, 0, 0, 0, 6))

        val doneDesc = tv(tr("Your image has been compressed successfully. You can now save, share or compress another file."), 12f, 0xFF9AA4B8.toInt())
        doneDesc.gravity = Gravity.CENTER
        c.addView(doneDesc, lpm(MATCH, WRAP, 0, 0, 0, 20))

        val compareRow = LinearLayout(this)
        compareRow.orientation = LinearLayout.HORIZONTAL

        val origCard = LinearLayout(this)
        origCard.orientation = LinearLayout.VERTICAL
        origCard.background = rr(0xFF131B2E.toInt(), dpf(14))
        origCard.setPadding(dp(6), dp(6), dp(6), dp(6))
        val origImgBox = FrameLayout(this)
        val origImg = ImageView(this)
        origImg.scaleType = ImageView.ScaleType.CENTER_CROP
        selectedImageUri?.let { origImg.setImageURI(it) }
        origImgBox.addView(origImg, FrameLayout.LayoutParams(MATCH, dp(120)))
        val origTag = tv(tr("Original"), 10f, Color.WHITE, true)
        origTag.setPadding(dp(8), dp(4), dp(8), dp(4))
        origTag.background = rr(0x99000000.toInt(), dpf(10))
        val origTagLp = FrameLayout.LayoutParams(WRAP, WRAP)
        origTagLp.gravity = Gravity.TOP or Gravity.START
        origTagLp.topMargin = dp(8)
        origTagLp.leftMargin = dp(8)
        origImgBox.addView(origTag, origTagLp)
        origCard.addView(origImgBox, lp(MATCH, dp(120)))
        origCard.addView(tv(formatSize(originalFileSize), 13f, Color.WHITE, true), lpm(WRAP, WRAP, 8, 8, 0, 0))
        origCard.addView(tv(originalBitmapW.toString() + " x " + originalBitmapH.toString(), 10f, 0xFF7C8699.toInt()), lpm(WRAP, WRAP, 8, 2, 0, 8))

        val compCard = LinearLayout(this)
        compCard.orientation = LinearLayout.VERTICAL
        val compBorder = GradientDrawable()
        compBorder.setColor(0xFF131B2E.toInt())
        compBorder.cornerRadius = dpf(14)
        compBorder.setStroke(dp(2), 0xFF3DDC84.toInt())
        compCard.background = compBorder
        compCard.setPadding(dp(6), dp(6), dp(6), dp(6))
        val compImgBox = FrameLayout(this)
        val compImg = ImageView(this)
        compImg.scaleType = ImageView.ScaleType.CENTER_CROP
        val cf = compressedFile
        if (cf != null) {
            compImg.setImageURI(Uri.fromFile(cf))
        }
        compImgBox.addView(compImg, FrameLayout.LayoutParams(MATCH, dp(120)))
        val compTag = tv(tr("Compressed"), 10f, Color.WHITE, true)
        compTag.setPadding(dp(8), dp(4), dp(8), dp(4))
        compTag.background = rr(0xFF1E7A45.toInt(), dpf(10))
        val compTagLp = FrameLayout.LayoutParams(WRAP, WRAP)
        compTagLp.gravity = Gravity.TOP or Gravity.START
        compTagLp.topMargin = dp(8)
        compTagLp.leftMargin = dp(8)
        compImgBox.addView(compTag, compTagLp)
        compCard.addView(compImgBox, lp(MATCH, dp(120)))

        val compInfoRow = LinearLayout(this)
        compInfoRow.orientation = LinearLayout.HORIZONTAL
        compInfoRow.gravity = Gravity.CENTER_VERTICAL
        val compSizeLp = lp(0, WRAP)
        compSizeLp.weight = 1f
        compInfoRow.addView(tv(formatSize(compressedFileSize), 13f, Color.WHITE, true), compSizeLp)
        val reduction = if (originalFileSize > 0) (100 - (compressedFileSize * 100 / originalFileSize)).toInt().coerceIn(0, 99) else 0
        val reducedBadge = tv("-" + reduction.toString() + "%", 10f, Color.WHITE, true)
        reducedBadge.setPadding(dp(6), dp(3), dp(6), dp(3))
        reducedBadge.background = rr(0xFF1E7A45.toInt(), dpf(10))
        compInfoRow.addView(reducedBadge, lp(WRAP, WRAP))
        compCard.addView(compInfoRow, lpm(MATCH, WRAP, 8, 8, 0, 0))
        compCard.addView(tv(originalBitmapW.toString() + " x " + originalBitmapH.toString(), 10f, 0xFF7C8699.toInt()), lpm(WRAP, WRAP, 8, 2, 0, 8))

        val origLp2 = lp(0, WRAP)
        origLp2.weight = 1f
        origLp2.marginEnd = dp(8)
        compareRow.addView(origCard, origLp2)
        val compLp2 = lp(0, WRAP)
        compLp2.weight = 1f
        compareRow.addView(compCard, compLp2)

        c.addView(compareRow, lpm(MATCH, WRAP, 0, 0, 0, 16))

        val statsCard = sectionCard()
        val statsRow = LinearLayout(this)
        statsRow.orientation = LinearLayout.HORIZONTAL
        statsRow.gravity = Gravity.CENTER_VERTICAL

        val ringBox2 = FrameLayout(this)
        val ring2 = CircularProgressView(this)
        ring2.progress = reduction
        ringBox2.addView(ring2, FrameLayout.LayoutParams(dp(60), dp(60)))
        statsRow.addView(ringBox2, lp(dp(60), dp(60)))

        val statsTextBox = LinearLayout(this)
        statsTextBox.orientation = LinearLayout.VERTICAL
        statsTextBox.addView(tv(tr("File Size Reduced"), 11f, 0xFF7C8699.toInt()))
        statsTextBox.addView(tv(reduction.toString() + "%", 20f, 0xFF3DDC84.toInt(), true), lpm(WRAP, WRAP, 0, 2, 0, 0))
        statsTextBox.addView(tv(formatSize(originalFileSize) + " -> " + formatSize(compressedFileSize), 11f, 0xFF7C8699.toInt()), lpm(WRAP, WRAP, 0, 2, 0, 0))
        val statsLp = lp(0, WRAP)
        statsLp.weight = 1f
        statsLp.marginStart = dp(14)
        statsRow.addView(statsTextBox, statsLp)

        val qualBox = LinearLayout(this)
        qualBox.orientation = LinearLayout.VERTICAL
        qualBox.gravity = Gravity.CENTER
        val leafIcon = tv(tr("OK"), 14f, Color.WHITE, true)
        leafIcon.gravity = Gravity.CENTER
        leafIcon.background = rr(0xFF1E7A45.toInt(), dpf(18))
        qualBox.addView(leafIcon, lp(dp(36), dp(36)))
        qualBox.addView(tv(tr("Quality Preserved"), 10f, Color.WHITE, true), lpm(WRAP, WRAP, 0, 6, 0, 0))
        statsRow.addView(qualBox, lp(WRAP, WRAP))

        statsCard.addView(statsRow, lp(MATCH, WRAP))
        c.addView(statsCard, lpm(MATCH, WRAP, 0, 0, 0, 20))

        val actionRow = LinearLayout(this)
        actionRow.orientation = LinearLayout.HORIZONTAL

        val shareBtn = LinearLayout(this)
        shareBtn.orientation = LinearLayout.VERTICAL
        shareBtn.gravity = Gravity.CENTER
        shareBtn.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(16))
        shareBtn.setPadding(dp(10), dp(12), dp(10), dp(12))
        val shareTitle = tv(tr("Share"), 13f, Color.WHITE, true)
        shareTitle.gravity = Gravity.CENTER
        shareBtn.addView(shareTitle)
        val shareSub = tv(tr("Share image"), 9f, 0xDDFFFFFF.toInt())
        shareSub.gravity = Gravity.CENTER
        shareBtn.addView(shareSub, lpm(WRAP, WRAP, 0, 2, 0, 0))
        shareBtn.isClickable = true
        shareBtn.setOnClickListener { shareCompressedFile() }
        val shareLp = lp(0, WRAP)
        shareLp.weight = 1f
        shareLp.marginEnd = dp(8)
        actionRow.addView(shareBtn, shareLp)

        val saveBtn = LinearLayout(this)
        saveBtn.orientation = LinearLayout.VERTICAL
        saveBtn.gravity = Gravity.CENTER
        saveBtn.background = rr(0xFF1C2538.toInt(), dpf(16))
        saveBtn.setPadding(dp(10), dp(12), dp(10), dp(12))
        val saveTitle = tv(tr("Save"), 13f, Color.WHITE, true)
        saveTitle.gravity = Gravity.CENTER
        saveBtn.addView(saveTitle)
        val saveSub = tv(tr("Save to device"), 9f, 0xFF9AA4B8.toInt())
        saveSub.gravity = Gravity.CENTER
        saveBtn.addView(saveSub, lpm(WRAP, WRAP, 0, 2, 0, 0))
        saveBtn.isClickable = true
        saveBtn.setOnClickListener { saveCompressedFile() }
        val saveLp = lp(0, WRAP)
        saveLp.weight = 1f
        saveLp.marginEnd = dp(8)
        actionRow.addView(saveBtn, saveLp)

        val againBtn = LinearLayout(this)
        againBtn.orientation = LinearLayout.VERTICAL
        againBtn.gravity = Gravity.CENTER
        againBtn.background = rr(0xFF1C2538.toInt(), dpf(16))
        againBtn.setPadding(dp(10), dp(12), dp(10), dp(12))
        val againTitle = tv(tr("Compress Again"), 13f, Color.WHITE, true)
        againTitle.gravity = Gravity.CENTER
        againBtn.addView(againTitle)
        val againSub = tv(tr("Start a new process"), 9f, 0xFF9AA4B8.toInt())
        againSub.gravity = Gravity.CENTER
        againBtn.addView(againSub, lpm(WRAP, WRAP, 0, 2, 0, 0))
        againBtn.isClickable = true
        againBtn.setOnClickListener { setScreen("compress_image") }
        val againLp = lp(0, WRAP)
        againLp.weight = 1f
        actionRow.addView(againBtn, againLp)

        c.addView(actionRow, lpm(MATCH, WRAP, 0, 0, 0, 14))

        val backHomeBtn = LinearLayout(this)
        backHomeBtn.orientation = LinearLayout.VERTICAL
        backHomeBtn.gravity = Gravity.CENTER
        backHomeBtn.background = grad(intArrayOf(0xFF2E8BFF.toInt(), 0xFF8A2BFF.toInt()), dpf(24))
        backHomeBtn.setPadding(dp(16), dp(14), dp(16), dp(14))
        val bhTitle = tv(tr("Back to Home"), 15f, Color.WHITE, true)
        bhTitle.gravity = Gravity.CENTER
        backHomeBtn.addView(bhTitle)
        val bhSub = tv(tr("Go to main screen"), 11f, 0xDDFFFFFF.toInt())
        bhSub.gravity = Gravity.CENTER
        backHomeBtn.addView(bhSub, lpm(WRAP, WRAP, 0, 2, 0, 0))
        backHomeBtn.isClickable = true
        backHomeBtn.setOnClickListener { setScreen("home") }
        c.addView(backHomeBtn, lp(MATCH, WRAP))

        scroll.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))
        outer.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
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
