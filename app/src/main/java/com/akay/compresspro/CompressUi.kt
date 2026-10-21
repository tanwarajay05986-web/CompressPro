package com.akay.compresspro

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

// the Compression Settings screen (image / PDF) and the compress run
class CompressUi(private val a: MainActivity) {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val ui = Handler(Looper.getMainLooper())

    private var items = ArrayList<Item>()
    private var focus = 0
    private var pdf = false
    private val o = Opts()
    private var updating = false
    private var estVer = 0

    @Volatile
    private var cancelled = false

    private lateinit var thumbRow: LinearLayout
    private lateinit var estNum: TextView
    private lateinit var estUnit: TextView
    private lateinit var savedText: TextView
    private lateinit var origText: TextView
    private lateinit var estBar: HBar
    private lateinit var qPill: TextView
    private lateinit var qSeek: QSeek
    private lateinit var dPill: TextView
    private lateinit var hField: EditText
    private lateinit var wField: EditText
    private lateinit var lockSwitch: SwitchView
    private lateinit var specialBtn: FrameLayout
    private lateinit var specialCheck: IconView
    private var qCard: View? = null
    private var qShield: View? = null
    private var fCard: View? = null
    private var fShield: View? = null
    private val qChips = ArrayList<Chip>()
    private val dChips = ArrayList<Chip>()
    private val fChips = ArrayList<Chip>()

    private class Chip(val box: LinearLayout, val tv: TextView, val icon: IconView?, val checkOnSel: Boolean)

    private val qVals = intArrayOf(30, 60, 90)
    private val dVals = intArrayOf(25, 50, 75)

    // ---------- helpers ----------

    private fun tr(s: String): String = T.t(s)

    private fun dp(v: Int): Int = a.dp(v)

    private fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams = a.lp(w, h, wt)

    private fun lpm(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        a.lpm(w, h, l, t, r, b)

    private fun cur(): Item = items[focus.coerceIn(0, items.size - 1)]

    fun leave() {
        ui.removeCallbacks(estRun)
        estVer++
        items.clear()
    }

    private fun showKeyboard(v: View) {
        val imm = a.getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(v: View) {
        val imm = a.getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }

    // ---------- the screen ----------

    fun show(isPdf: Boolean, list: List<Item>) {
        pdf = isPdf
        items = ArrayList(list)
        focus = 0
        o.quality = qVals[Prefs.quality().coerceIn(0, 2)]
        o.scale = 100
        o.exact = false
        o.exactW = 0
        o.exactH = 0
        o.fmt = 0
        o.pdf = isPdf
        o.special = false
        qChips.clear()
        dChips.clear()
        fChips.clear()
        qCard = null
        qShield = null
        fCard = null
        fShield = null
        a.screen = "compress"

        val c = LinearLayout(a)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(12), dp(16), dp(14))
        c.addView(header(), lp(MATCH, WRAP))
        c.addView(thumbStrip(), lpm(MATCH, WRAP, 0, 14, 0, 0))
        c.addView(estCard(), lpm(MATCH, WRAP, 0, 14, 0, 0))
        c.addView(qualityCard(), lpm(MATCH, WRAP, 0, 12, 0, 0))
        c.addView(dimCard(), lpm(MATCH, WRAP, 0, 12, 0, 0))
        if (!pdf) c.addView(formatCard(), lpm(MATCH, WRAP, 0, 12, 0, 0))

        val sv = ScrollView(a)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(c)

        val root = LinearLayout(a)
        root.orientation = LinearLayout.VERTICAL
        root.addView(sv, lp(MATCH, 0, 1f))
        root.addView(compressButton(), lpm(MATCH, 64, 16, 8, 16, 10))
        root.addView(a.navBar(0), lp(MATCH, WRAP))
        a.setScreen(root, P.navBg)

        syncFields()
        updateQualityUi()
        updateDimUi()
        updateFmtUi()
        applySpecialUi()
        requestEstimate()
    }

    private fun header(): View {
        val r = LinearLayout(a)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        val back = FrameLayout(a)
        val bd = GradientDrawable()
        bd.shape = GradientDrawable.OVAL
        bd.setColor(P.card)
        bd.setStroke(dp(1), P.cardEdge)
        back.background = bd
        back.addView(IconView(a, K.CHEV_L, P.text), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        back.setOnClickListener { a.showHome() }
        r.addView(back, lp(dp(44), dp(44)))

        val col = LinearLayout(a)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), 0, dp(6), 0)
        col.addView(a.tv(tr("Compression Settings"), 18f, P.text, 2))
        col.addView(a.tv(tr("Adjust settings to get the best balance between quality and file size."), 11.5f, P.sub))
        r.addView(col, lp(0, WRAP, 1f))
        r.addView(SettingsArt(a), lp(dp(72), dp(64)))
        return r
    }

    // ---------- thumbnails ----------

    private fun thumbStrip(): View {
        val hs = HorizontalScrollView(a)
        hs.isHorizontalScrollBarEnabled = false
        thumbRow = LinearLayout(a)
        thumbRow.orientation = LinearLayout.HORIZONTAL
        hs.addView(thumbRow)
        buildThumbs()
        return hs
    }

    private fun buildThumbs() {
        thumbRow.removeAllViews()
        for (i in items.indices) {
            val l = lp(dp(60), dp(60))
            l.rightMargin = dp(8)
            thumbRow.addView(thumb(i), l)
        }
    }

    private fun thumb(i: Int): View {
        val sel = i == focus
        val f = FrameLayout(a)
        f.background = if (sel) a.rr(Color.TRANSPARENT, 16, P.blue, 2) else a.rr(Color.TRANSPARENT, 16, P.cardEdge, 1)
        val iv = ImageView(a)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.setImageBitmap(items[i].thumb)
        iv.background = a.rr(P.card, 13)
        iv.clipToOutline = true
        val m = dp(3)
        val ip = FrameLayout.LayoutParams(MATCH, MATCH)
        ip.setMargins(m, m, m, m)
        f.addView(iv, ip)
        if (sel) {
            val badge = FrameLayout(a)
            val bd = GradientDrawable()
            bd.shape = GradientDrawable.OVAL
            bd.setColor(P.blue)
            badge.background = bd
            badge.addView(IconView(a, K.CHECK, Color.WHITE), FrameLayout.LayoutParams(dp(12), dp(12), Gravity.CENTER))
            val bp = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.BOTTOM or Gravity.END)
            bp.setMargins(0, 0, dp(2), dp(2))
            f.addView(badge, bp)
        }
        f.setOnClickListener {
            focus = i
            buildThumbs()
            syncFields()
            updateDimUi()
        }
        return f
    }

    // ---------- cards ----------

    private fun card(): LinearLayout {
        val b = LinearLayout(a)
        b.orientation = LinearLayout.VERTICAL
        b.background = a.rr(P.card, 20, P.cardEdge, 1)
        b.setPadding(dp(14), dp(14), dp(14), dp(14))
        return b
    }

    private fun pill(text: String, fg: Int, bg: Int): TextView {
        val t = a.tv(text, 15f, fg, 2)
        t.setPadding(dp(12), dp(7), dp(12), dp(7))
        t.background = a.rr(bg, 14)
        return t
    }

    private fun cardHead(
        icon: Int, tileBg: GradientDrawable, iconColor: Int, title: String, sub: String, pill: View?
    ): View {
        val r = LinearLayout(a)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        val t = FrameLayout(a)
        t.background = tileBg
        t.addView(IconView(a, icon, iconColor), FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        r.addView(t, lp(dp(44), dp(44)))
        val col = LinearLayout(a)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), 0, dp(6), 0)
        col.addView(a.tv(tr(title), 17f, P.text, 2))
        col.addView(a.tv(tr(sub), 12f, P.sub))
        r.addView(col, lp(0, WRAP, 1f))
        if (pill != null) r.addView(pill)
        return r
    }

    private fun mkChip(label: String, icon: Int, checkOnSel: Boolean, onClick: () -> Unit): Chip {
        val b = LinearLayout(a)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER
        b.setPadding(dp(4), 0, dp(4), 0)
        var ic: IconView? = null
        if (icon >= 0) {
            ic = IconView(a, icon, P.sub)
            b.addView(ic, lpm(15, 15, 0, 0, 6, 0))
        }
        val t = a.tv(label, 13f, P.text, 1)
        t.maxLines = 1
        t.setSingleLine(true)
        b.addView(t)
        b.setOnClickListener { onClick() }
        return Chip(b, t, ic, checkOnSel)
    }

    private fun setSel(ch: Chip, sel: Boolean) {
        ch.box.background = if (sel) a.grad(P.chipSel, GradientDrawable.Orientation.LEFT_RIGHT, 14)
        else a.rr(P.chip, 14)
        ch.tv.setTextColor(if (sel) Color.WHITE else P.text)
        ch.tv.typeface = if (sel) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
        ch.icon?.tint(if (sel) Color.WHITE else P.sub)
        if (ch.checkOnSel) ch.icon?.visibility = if (sel) View.VISIBLE else View.GONE
    }

    private fun chipRow(chips: List<Chip>, w: FloatArray): LinearLayout {
        val r = LinearLayout(a)
        r.orientation = LinearLayout.HORIZONTAL
        for (i in chips.indices) {
            val l = lp(0, dp(44), w[i])
            l.leftMargin = if (i == 0) 0 else dp(8)
            r.addView(chips[i].box, l)
        }
        return r
    }

    // ----- estimated size and the Special 200 KB button -----

    private fun estCard(): View {
        val card = card()
        val top = LinearLayout(a)
        top.orientation = LinearLayout.HORIZONTAL

        val left = LinearLayout(a)
        left.orientation = LinearLayout.VERTICAL
        val lab = LinearLayout(a)
        lab.orientation = LinearLayout.HORIZONTAL
        lab.gravity = Gravity.CENTER_VERTICAL
        lab.addView(IconView(a, K.DATABASE, P.iconSoft), lp(dp(18), dp(18)))
        val lt = a.tv(tr("ESTIMATED SIZE"), 12f, P.sub, 2)
        lt.letterSpacing = 0.08f
        lt.setPadding(dp(8), 0, 0, 0)
        lab.addView(lt)
        left.addView(lab)
        val numRow = LinearLayout(a)
        numRow.orientation = LinearLayout.HORIZONTAL
        numRow.gravity = Gravity.BOTTOM
        estNum = a.tv("0", 36f, P.text, 2)
        estUnit = a.tv(" MB", 20f, P.blue, 2)
        estUnit.setPadding(0, 0, 0, dp(5))
        numRow.addView(estNum)
        numRow.addView(estUnit)
        left.addView(numRow, lpm(WRAP, WRAP, 0, 4, 0, 0))
        left.addView(a.tv(tr("(After Compression)"), 12f, P.sub))
        top.addView(left, lp(0, WRAP, 1f))

        val right = LinearLayout(a)
        right.orientation = LinearLayout.VERTICAL
        right.gravity = Gravity.END
        val pl = LinearLayout(a)
        pl.orientation = LinearLayout.HORIZONTAL
        pl.gravity = Gravity.CENTER_VERTICAL
        pl.background = a.rr(0x2219E39A, 14, 0x6619E39A, 1)
        pl.setPadding(dp(10), dp(6), dp(12), dp(6))
        pl.addView(IconView(a, K.LEAF, P.green), lpm(14, 14, 0, 0, 6, 0))
        savedText = a.tv("-0% " + tr("Saved"), 13f, P.green, 1)
        pl.addView(savedText)
        right.addView(pl)
        origText = a.tv(tr("Original:") + " 0 MB", 13f, P.sub)
        right.addView(origText, lpm(WRAP, WRAP, 0, 18, 0, 0))
        top.addView(right)
        card.addView(top)

        estBar = HBar(a)
        card.addView(estBar, lpm(MATCH, 8, 0, 12, 0, 0))
        card.addView(specialButton(), lpm(MATCH, 46, 0, 12, 0, 0))
        return card
    }

    // free "Special 200 KB" mode (crown): the file is made about 200 KB, only dimensions can be changed
    private fun specialButton(): View {
        val f = FrameLayout(a)
        specialBtn = f

        val row = LinearLayout(a)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.addView(IconView(a, K.CROWN, Color.WHITE), lp(dp(22), dp(22)))
        val t = a.tv(tr("Special 200 KB"), 15f, Color.WHITE, 2)
        t.setPadding(dp(8), 0, 0, 0)
        row.addView(t)
        specialCheck = IconView(a, K.CHECK, Color.WHITE)
        row.addView(specialCheck, lpm(16, 16, 8, 0, 0, 0))
        f.addView(row, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))

        val ch = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.END or Gravity.CENTER_VERTICAL)
        ch.rightMargin = dp(14)
        f.addView(IconView(a, K.CHEV_R, Color.WHITE), ch)
        f.setOnClickListener { toggleSpecial() }
        return f
    }

    private fun toggleSpecial() {
        o.special = !o.special
        applySpecialUi()
        a.toast(
            tr(
                if (o.special) "Special mode ON: about 200 KB file. Only dimensions can be changed."
                else "Special mode OFF"
            )
        )
        requestEstimate()
    }

    // when Special is ON, the Quality and Format cards go dim and cannot be used
    private fun applySpecialUi() {
        val on = o.special
        specialCheck.visibility = if (on) View.VISIBLE else View.GONE
        val g = a.grad(intArrayOf(0xFF1E8BFF.toInt(), 0xFF7A3DFF.toInt()), GradientDrawable.Orientation.LEFT_RIGHT, 23)
        if (on) g.setStroke(dp(2), Color.WHITE)
        specialBtn.background = g
        qCard?.alpha = if (on) 0.4f else 1f
        qShield?.visibility = if (on) View.VISIBLE else View.GONE
        fCard?.alpha = if (on) 0.4f else 1f
        fShield?.visibility = if (on) View.VISIBLE else View.GONE
        updateQualityUi()
        updateFmtUi()
    }

    // ----- quality -----

    private fun qualityCard(): View {
        val card = card()
        qPill = pill("60%", 0xFFB388FF.toInt(), 0x336B3DFF)
        card.addView(
            cardHead(
                K.SLIDERS, a.grad(intArrayOf(0xFF5B3DF5.toInt(), 0xFF7C4DFF.toInt()), GradientDrawable.Orientation.TL_BR, 14),
                Color.WHITE, "Quality", "Adjust the compression quality.", qPill
            )
        )
        qSeek = QSeek(a)
        qSeek.value = o.quality
        qSeek.onChange = { v ->
            o.quality = v
            updateQualityUi()
            requestEstimate()
        }
        qSeek.onDone = { requestEstimate() }
        card.addView(qSeek, lpm(MATCH, 40, 0, 10, 0, 0))

        val names = arrayOf("Low (30%)", "Medium (60%)", "High (90%)")
        for (i in 0..2) {
            qChips.add(mkChip(tr(names[i]), -1, false) {
                o.quality = qVals[i]
                qSeek.value = qVals[i]
                updateQualityUi()
                requestEstimate()
            })
        }
        card.addView(chipRow(qChips, floatArrayOf(1f, 1.2f, 1f)), lpm(MATCH, WRAP, 0, 8, 0, 0))

        val wrap = FrameLayout(a)
        wrap.addView(card, FrameLayout.LayoutParams(MATCH, WRAP))
        val shield = View(a)
        shield.isClickable = true
        shield.visibility = View.GONE
        wrap.addView(shield, FrameLayout.LayoutParams(MATCH, MATCH))
        qCard = card
        qShield = shield
        return wrap
    }

    private fun updateQualityUi() {
        qPill.text = if (o.special) tr("Auto") else o.quality.toString() + "%"
        qSeek.value = o.quality
        for (i in 0..2) setSel(qChips[i], !o.special && o.quality == qVals[i])
    }

    // ----- dimensions -----

    private fun field(isWidth: Boolean): Pair<FrameLayout, EditText> {
        val f = FrameLayout(a)
        f.background = a.rr(P.field, 12, P.cardEdge, 1)
        val e = EditText(a)
        e.background = null
        e.setTextColor(P.text)
        e.textSize = 15f
        e.inputType = InputType.TYPE_CLASS_NUMBER
        e.imeOptions = EditorInfo.IME_ACTION_DONE
        e.setSingleLine(true)
        e.gravity = Gravity.CENTER_VERTICAL
        e.setPadding(dp(10), 0, dp(26), 0)
        e.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!updating) onTyped(isWidth, s?.toString() ?: "")
            }
        })
        e.setOnEditorActionListener { v, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                syncFields()
                hideKeyboard(v)
                v.clearFocus()
            }
            true
        }
        e.setOnFocusChangeListener { _, has -> if (!has) syncFields() }
        f.addView(e, FrameLayout.LayoutParams(MATCH, MATCH))
        val px = a.tv("px", 11f, P.sub)
        val pl = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER_VERTICAL or Gravity.END)
        pl.rightMargin = dp(8)
        f.addView(px, pl)
        return Pair(f, e)
    }

    private fun dimCard(): View {
        val card = card()
        dPill = pill("100%", P.cyan, 0x2600A8FF)
        card.addView(
            cardHead(
                K.RESIZE, a.rr(0x2200C8FF, 14, 0xFF00C8FF.toInt(), 1),
                P.cyan, "Dimensions", "Set your desired width and height.", dPill
            )
        )

        val row = LinearLayout(a)
        row.orientation = LinearLayout.HORIZONTAL

        val colA = LinearLayout(a)
        colA.orientation = LinearLayout.VERTICAL
        colA.addView(a.tv(tr("Height"), 13f, P.sub))
        val fh = field(false)
        hField = fh.second
        colA.addView(fh.first, lpm(MATCH, 46, 0, 4, 0, 0))
        row.addView(colA, lp(0, WRAP, 1f))

        val x = a.tv("\u00D7", 18f, P.blue, 2)
        x.setPadding(dp(6), dp(24), dp(6), 0)
        row.addView(x)

        val colB = LinearLayout(a)
        colB.orientation = LinearLayout.VERTICAL
        colB.addView(a.tv(tr("Width"), 13f, P.sub))
        val fw = field(true)
        wField = fw.second
        colB.addView(fw.first, lpm(MATCH, 46, 0, 4, 0, 0))
        row.addView(colB, lp(0, WRAP, 1f))

        // lock aspect ratio
        val colC = LinearLayout(a)
        colC.orientation = LinearLayout.VERTICAL
        val lr = LinearLayout(a)
        lr.orientation = LinearLayout.HORIZONTAL
        lr.gravity = Gravity.CENTER_VERTICAL
        lr.addView(IconView(a, K.LOCK, P.iconSoft), lp(dp(18), dp(18)))
        val lt = LinearLayout(a)
        lt.orientation = LinearLayout.VERTICAL
        lt.setPadding(dp(6), 0, 0, 0)
        lt.addView(a.tv(tr("Lock"), 11f, P.sub))
        lt.addView(a.tv(tr("Aspect Ratio"), 11f, P.sub))
        lr.addView(lt)
        colC.addView(lr)
        lockSwitch = SwitchView(a)
        lockSwitch.on = true
        if (pdf) {
            lockSwitch.isEnabled = false
            lockSwitch.alpha = 0.5f
        }
        lockSwitch.onToggle = { on ->
            val item = cur()
            if (!on) {
                val d = Engine.outDims(item, o)
                o.exact = true
                o.exactW = d[0]
                o.exactH = d[1]
            } else {
                o.exact = false
                if (o.exactW > 0) o.scale = Math.round(o.exactW * 100.0 / item.w).toInt().coerceIn(1, 100)
                syncFields()
            }
            updateDimUi()
            requestEstimate()
        }
        val sl = lpm(44, 24, 0, 6, 0, 0)
        sl.gravity = Gravity.END
        colC.addView(lockSwitch, sl)
        row.addView(colC, lpm(88, WRAP, 8, 0, 0, 0))
        card.addView(row, lpm(MATCH, WRAP, 0, 10, 0, 0))

        for (i in 0..2) {
            dChips.add(mkChip(dVals[i].toString() + "%", -1, false) {
                o.scale = dVals[i]
                o.exact = false
                if (!pdf) lockSwitch.on = true
                syncFields()
                updateDimUi()
                requestEstimate()
            })
        }
        dChips.add(mkChip(tr("Custom"), K.PENCIL, false) {
            wField.requestFocus()
            wField.setSelection(wField.text.length)
            showKeyboard(wField)
        })
        card.addView(chipRow(dChips, floatArrayOf(1f, 1f, 1f, 1.4f)), lpm(MATCH, WRAP, 0, 10, 0, 0))
        return card
    }

    // user typed a new width or height
    private fun onTyped(isWidth: Boolean, text: String) {
        val v = text.toIntOrNull() ?: return
        if (v <= 0 || items.isEmpty()) return
        val item = cur()
        updating = true
        if (!pdf && !lockSwitch.on) {
            o.exact = true
            if (o.exactW <= 0 || o.exactH <= 0) {
                val d = Engine.outDims(item, o)
                o.exactW = d[0]
                o.exactH = d[1]
            }
            if (isWidth) o.exactW = v else o.exactH = v
        } else {
            val base = if (isWidth) item.w else item.h
            o.exact = false
            o.scale = Math.round(v * 100.0 / base).toInt().coerceIn(1, 100)
            val d = Engine.outDims(item, o)
            if (isWidth) hField.setText(d[1].toString()) else wField.setText(d[0].toString())
        }
        updating = false
        updateDimUi()
        requestEstimate()
    }

    // puts the real numbers in both fields
    private fun syncFields() {
        if (items.isEmpty()) return
        val d = Engine.outDims(cur(), o)
        updating = true
        hField.setText(d[1].toString())
        wField.setText(d[0].toString())
        updating = false
    }

    private fun updateDimUi() {
        if (items.isEmpty()) return
        val item = cur()
        val pct = if (o.exact && o.exactW > 0) Math.round(o.exactW * 100.0 / item.w).toInt() else o.scale
        dPill.text = "$pct%"
        var sel = 3
        if (!o.exact) {
            for (i in 0..2) if (o.scale == dVals[i]) sel = i
        }
        for (i in 0..3) setSel(dChips[i], i == sel)
    }

    // ----- format -----

    private fun formatCard(): View {
        val card = card()
        card.addView(
            cardHead(
                K.IMAGE, a.grad(intArrayOf(0xFF4B2DB8.toInt(), 0xFF6C3DF0.toInt()), GradientDrawable.Orientation.TL_BR, 14),
                Color.WHITE, "Format", "Choose output format for your image.", null
            )
        )
        val names = arrayOf("JPG", "PNG", "WEBP", "Original")
        for (i in 0..3) {
            val label = if (i == 3) tr("Original") else names[i]
            fChips.add(mkChip(label, if (i == 3) K.REFRESH else K.CHECK, i < 3) {
                o.fmt = i
                updateFmtUi()
                requestEstimate()
            })
        }
        card.addView(chipRow(fChips, floatArrayOf(1.1f, 1f, 1f, 1.3f)), lpm(MATCH, WRAP, 0, 12, 0, 0))

        val wrap = FrameLayout(a)
        wrap.addView(card, FrameLayout.LayoutParams(MATCH, WRAP))
        val shield = View(a)
        shield.isClickable = true
        shield.visibility = View.GONE
        wrap.addView(shield, FrameLayout.LayoutParams(MATCH, MATCH))
        fCard = card
        fShield = shield
        return wrap
    }

    private fun updateFmtUi() {
        if (pdf) return
        for (i in 0..3) setSel(fChips[i], if (o.special) i == 0 else o.fmt == i)
    }

    // ----- compress button -----

    private fun compressButton(): View {
        val b = LinearLayout(a)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER
        b.background = a.grad(P.grad3, GradientDrawable.Orientation.LEFT_RIGHT, 32)
        b.elevation = dp(6).toFloat()
        b.addView(IconView(a, K.DOWNLOAD, Color.WHITE), lp(dp(30), dp(30)))
        val col = LinearLayout(a)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(14), 0, 0, 0)
        col.addView(a.tv(tr(if (pdf) "Compress PDF" else "Compress Image"), 18f, Color.WHITE, 2))
        col.addView(a.tv(tr("Tap to start compression"), 12f, 0xCCFFFFFF.toInt()))
        b.addView(col)
        b.setOnClickListener { startCompress() }
        return b
    }

    // ---------- estimate ----------

    private val estRun = Runnable { runEstimate() }

    private fun requestEstimate() {
        ui.removeCallbacks(estRun)
        ui.postDelayed(estRun, 150)
    }

    private fun runEstimate() {
        if (items.isEmpty()) return
        val ver = ++estVer
        val its = ArrayList(items)
        val oc = o.copy()
        Thread {
            var tot = 0L
            var orig = 0L
            for (item in its) {
                tot += Engine.estimate(item, oc)
                orig += item.size
            }
            a.runOnUiThread {
                if (ver == estVer && a.screen == "compress") showEstimate(tot, orig)
            }
        }.start()
    }

    private fun showEstimate(est: Long, orig: Long) {
        val (v, u) = Engine.fmtSize(est)
        estNum.text = v
        estUnit.text = " $u"
        val (ov, ou) = Engine.fmtSize(orig)
        origText.text = tr("Original:") + " $ov $ou"
        val saved = if (orig > 0) ((orig - est) * 100.0 / orig).toInt().coerceIn(0, 99) else 0
        savedText.text = "-$saved% " + tr("Saved")
        estBar.frac = if (orig > 0) (est.toFloat() / orig).coerceIn(0.03f, 1f) else 0.1f
    }

    // ---------- compress run (a simple progress box; the full design comes in Stage 3) ----------

    private fun startCompress() {
        if (items.isEmpty()) return
        cancelled = false
        val its = ArrayList(items)
        val oc = o.copy()
        val n = its.size

        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(24), dp(16), dp(24), dp(8))
        val msg = a.tv("", 15f, Color.WHITE)
        val bar = ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = 100
        box.addView(msg)
        box.addView(bar, lpm(MATCH, WRAP, 0, 12, 0, 0))
        val dlg = AlertDialog.Builder(a)
            .setTitle(tr("Compressing…"))
            .setView(box)
            .setCancelable(false)
            .setNegativeButton(tr("Cancel")) { _, _ -> cancelled = true }
            .create()
        dlg.show()

        Thread {
            val dir = File(a.cacheDir, "out")
            dir.mkdirs()
            dir.listFiles()?.forEach { f -> f.delete() }
            var failed = 0
            for (idx in its.indices) {
                if (cancelled) break
                val item = its[idx]
                a.runOnUiThread { msg.text = tr("File") + " ${idx + 1}/$n  \u2022  " + item.name }
                val cb = { p: Float ->
                    a.runOnUiThread { bar.progress = (((idx + p) / n) * 100f).toInt() }
                }
                val f = try {
                    if (item.pdf) Engine.compressPdf(a, item, oc, dir, idx, { cancelled }, cb)
                    else Engine.compressImage(a, item, oc, dir, idx, { cancelled }, cb)
                } catch (e: Throwable) {
                    null
                }
                item.outFile = f
                if (f == null && !cancelled) failed++
            }
            a.runOnUiThread {
                try {
                    dlg.dismiss()
                } catch (e: Throwable) {
                }
                finishCompress(its, failed)
            }
        }.start()
    }

    private fun finishCompress(its: List<Item>, failed: Int) {
        if (cancelled) {
            a.toast(tr("Cancelled"))
            return
        }
        val done = its.filter { it.outFile != null }
        if (done.isEmpty()) {
            a.toast(tr("Compression failed"))
            return
        }
        var orig = 0L
        var outSz = 0L
        for (i in done) {
            orig += i.size
            outSz += i.outFile!!.length()
        }
        val pct = if (orig > 0) ((orig - outSz) * 100 / orig).toInt().coerceAtLeast(0) else 0
        val (ov, ou) = Engine.fmtSize(orig)
        val (cv, cu) = Engine.fmtSize(outSz)
        val sb = StringBuilder()
        sb.append(tr("Original size")).append(": ").append(ov).append(' ').append(ou).append('\n')
        sb.append(tr("Compressed size")).append(": ").append(cv).append(' ').append(cu)
        sb.append("  (-").append(pct).append("%)")
        if (outSz >= orig) sb.append("\n\n").append(tr("This file got bigger. Try a lower quality or smaller size."))
        if (failed > 0) sb.append("\n\n").append(tr("Some files failed")).append(": ").append(failed)

        val dlg = AlertDialog.Builder(a)
            .setTitle(tr("Compression Completed!"))
            .setMessage(sb.toString())
            .setPositiveButton(tr("Save"), null)
            .setNegativeButton(tr("Share"), null)
            .setNeutralButton(tr("Close")) { d, _ -> d.dismiss() }
            .create()
        dlg.show()
        val saveBtn = dlg.getButton(AlertDialog.BUTTON_POSITIVE)
        saveBtn.setOnClickListener {
            var ok = 0
            var where = ""
            for (i in done) {
                val r = Engine.saveToDevice(a, i.outFile!!, Prefs.storage())
                if (r != null) {
                    ok++
                    where = r
                }
            }
            if (ok > 0) {
                a.toast(tr("Saved to") + " " + where)
                saveBtn.isEnabled = false
            } else {
                a.toast(tr("Could not save"))
            }
        }
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            try {
                Engine.share(a, done.map { it.outFile!! })
            } catch (e: Throwable) {
                a.toast(tr("Could not share"))
            }
        }
    }
}
