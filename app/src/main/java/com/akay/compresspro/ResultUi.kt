package com.akay.compresspro

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

// the last screen: a progress circle while compressing, then a green tick and the result
class ResultUi(private val a: MainActivity) {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var items: List<Item> = emptyList()
    private var outItems: List<Item> = emptyList()
    private var pdf = false
    private var opts = Opts()

    @Volatile
    private var cancelled = false
    private var running = false
    private var done = false
    private var saved = false

    private lateinit var status: StatusView
    private lateinit var title1: TextView
    private lateinit var title2: TextView
    private lateinit var subText: TextView
    private lateinit var cancelTv: TextView
    private lateinit var origCard: PCard
    private lateinit var compCard: PCard
    private lateinit var ring: RingStat
    private lateinit var redPct: TextView
    private lateinit var fromTo: TextView
    private lateinit var qualSub: TextView
    private lateinit var shareBtn: Btn
    private lateinit var saveBtn: Btn
    private lateinit var againBtn: Btn

    private class PCard(
        val root: FrameLayout, val img: ImageView, val size: TextView, val dims: TextView, val pill: TextView?
    )

    private class Btn(val root: LinearLayout, val title: TextView, val sub: TextView)

    // ---------- helpers ----------

    private fun tr(s: String): String = T.t(s)

    private fun dp(v: Int): Int = a.dp(v)

    private fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams = a.lp(w, h, wt)

    private fun lpm(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        a.lpm(w, h, l, t, r, b)

    private fun sizeText(b: Long): String {
        val (v, u) = Engine.fmtSize(b)
        return "$v $u"
    }

    private fun gradTv(t: TextView, text: String, c1: Int, c2: Int) {
        t.text = text
        t.post {
            val w = t.paint.measureText(text)
            t.paint.shader = LinearGradient(0f, 0f, w, 0f, c1, c2, Shader.TileMode.CLAMP)
            t.invalidate()
        }
    }

    private fun dimsText(list: List<Item>): String {
        if (list.size > 1) return list.size.toString() + " " + tr("files")
        val i = list[0]
        return if (pdf) i.pages.toString() + " " + tr("pages") else "${i.w} \u00D7 ${i.h}"
    }

    // ---------- start ----------

    fun run(its: List<Item>, oc: Opts, isPdf: Boolean) {
        items = its
        opts = oc
        pdf = isPdf
        cancelled = false
        running = true
        done = false
        saved = false
        outItems = emptyList()
        a.screen = "result"
        buildScreen()
        startThread()
    }

    private fun goHome() {
        cancelled = true
        running = false
        a.showHome()
    }

    // the phone's back button
    fun onBack() {
        if (running) {
            cancelled = true
            running = false
            a.cui.reshow()
        } else {
            a.showHome()
        }
    }

    // ---------- screen ----------

    private fun buildScreen() {
        val c = LinearLayout(a)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(10), dp(18), dp(16))

        // top: logo, name and the home button
        val head = LinearLayout(a)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(a.logoTile(48), lp(dp(48), dp(48)))
        val hc = LinearLayout(a)
        hc.orientation = LinearLayout.VERTICAL
        hc.setPadding(dp(12), 0, dp(8), 0)
        hc.addView(a.brandRow(20f))
        hc.addView(a.tagline(11f))
        head.addView(hc, lp(0, WRAP, 1f))
        val home = FrameLayout(a)
        home.background = a.rr(P.card, 14, P.cardEdge, 1)
        home.addView(IconView(a, K.HOME, P.iconSoft), FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        home.setOnClickListener { goHome() }
        head.addView(home, lp(dp(46), dp(46)))
        c.addView(head, lp(MATCH, WRAP))

        // the circle: progress first, tick later
        status = StatusView(a)
        c.addView(status, lpm(MATCH, 112, 0, 4, 0, 0))

        val tr1 = LinearLayout(a)
        tr1.orientation = LinearLayout.HORIZONTAL
        title1 = a.tv("", 24f, P.text, 2)
        title2 = a.tv("", 24f, 0xFF00C8FF.toInt(), 2)
        tr1.addView(title1)
        tr1.addView(title2)
        val tl = lpm(WRAP, WRAP, 0, 8, 0, 0)
        tl.gravity = Gravity.CENTER_HORIZONTAL
        c.addView(tr1, tl)

        subText = a.tv("", 13f, P.sub)
        subText.gravity = Gravity.CENTER
        c.addView(subText, lpm(MATCH, WRAP, 8, 6, 8, 0))

        cancelTv = a.tv(tr("Cancel"), 14f, 0xFFE8245B.toInt(), 1)
        cancelTv.gravity = Gravity.CENTER
        cancelTv.setPadding(dp(16), dp(8), dp(16), dp(4))
        cancelTv.setOnClickListener { onBack() }
        val cl = lp(WRAP, WRAP)
        cl.gravity = Gravity.CENTER_HORIZONTAL
        c.addView(cancelTv, cl)

        // original and compressed pictures
        val pr = LinearLayout(a)
        pr.orientation = LinearLayout.HORIZONTAL
        pr.minimumHeight = dp(190)
        origCard = previewCard(false)
        compCard = previewCard(true)
        val lo = lp(0, MATCH, 1f)
        lo.rightMargin = dp(6)
        val lc = lp(0, MATCH, 1f)
        lc.leftMargin = dp(6)
        pr.addView(origCard.root, lo)
        pr.addView(compCard.root, lc)
        val plp = lp(MATCH, 0, 1f)
        plp.topMargin = dp(14)
        c.addView(pr, plp)

        c.addView(statsCard(), lpm(MATCH, WRAP, 0, 14, 0, 0))

        val row = LinearLayout(a)
        row.orientation = LinearLayout.HORIZONTAL
        shareBtn = actionBtn(0, "Share", "Share your compressed file") { doShare() }
        saveBtn = actionBtn(1, "Save", "Save to device") { doSave() }
        againBtn = actionBtn(2, "Compress Again", "Start a new process") { doAgain() }
        val l1 = lp(0, WRAP, 1.5f)
        val l2 = lp(0, WRAP, 1f)
        l2.leftMargin = dp(8)
        val l3 = lp(0, WRAP, 1.25f)
        l3.leftMargin = dp(8)
        row.addView(shareBtn.root, l1)
        row.addView(saveBtn.root, l2)
        row.addView(againBtn.root, l3)
        c.addView(row, lpm(MATCH, WRAP, 0, 12, 0, 0))

        c.addView(backHomeButton(), lpm(MATCH, 62, 0, 12, 0, 0))

        val sv = ScrollView(a)
        sv.isVerticalScrollBarEnabled = false
        sv.isFillViewport = true
        sv.addView(c)
        a.setScreen(sv, P.bg)

        showProcessing()
    }

    private fun previewCard(comp: Boolean): PCard {
        val root = FrameLayout(a)
        root.background = a.rr(P.card, 20, if (comp) P.green else P.cardEdge, if (comp) 2 else 1)
        root.clipToOutline = true
        val pad = dp(if (comp) 2 else 1)
        root.setPadding(pad, pad, pad, pad)

        val col = LinearLayout(a)
        col.orientation = LinearLayout.VERTICAL
        val img = ImageView(a)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        img.setBackgroundColor(P.chip)
        col.addView(img, lp(MATCH, 0, 1f))

        val info = LinearLayout(a)
        info.orientation = LinearLayout.HORIZONTAL
        info.gravity = Gravity.CENTER_VERTICAL
        info.setBackgroundColor(P.card)
        info.setPadding(dp(10), dp(8), dp(10), dp(8))
        val tile = FrameLayout(a)
        tile.background = a.rr(P.chip, 10)
        tile.addView(
            IconView(a, if (pdf) K.DOC else K.IMAGE, P.iconSoft),
            FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        )
        info.addView(tile, lp(dp(36), dp(36)))
        val tc = LinearLayout(a)
        tc.orientation = LinearLayout.VERTICAL
        tc.setPadding(dp(8), 0, dp(4), 0)
        val size = a.tv("", 15f, P.text, 2)
        size.maxLines = 1
        val dims = a.tv("", 11.5f, P.sub)
        dims.maxLines = 1
        tc.addView(size)
        tc.addView(dims)
        info.addView(tc, lp(0, WRAP, 1f))
        var pill: TextView? = null
        if (comp) {
            val pt = a.tv("", 12.5f, P.green, 2)
            pt.setPadding(dp(8), dp(4), dp(8), dp(4))
            pt.background = a.rr(0x2219E39A, 12)
            pt.visibility = View.GONE
            info.addView(pt)
            pill = pt
        }
        col.addView(info, lp(MATCH, WRAP))
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        val chip = a.tv(tr(if (comp) "Compressed" else "Original"), 12.5f, Color.WHITE, 1)
        chip.setPadding(dp(12), dp(5), dp(12), dp(5))
        chip.background = a.rr(if (comp) 0xFF14C87A.toInt() else 0x99596A8F.toInt(), 14)
        val cp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.START)
        cp.setMargins(dp(8), dp(8), 0, 0)
        root.addView(chip, cp)
        return PCard(root, img, size, dims, pill)
    }

    private fun statsCard(): View {
        val card = LinearLayout(a)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = a.rr(P.card, 20, P.cardEdge, 1)
        card.setPadding(dp(14), dp(12), dp(14), dp(12))

        // left: ring and the numbers
        val left = LinearLayout(a)
        left.orientation = LinearLayout.HORIZONTAL
        left.gravity = Gravity.CENTER_VERTICAL
        val rb = FrameLayout(a)
        ring = RingStat(a)
        rb.addView(ring, FrameLayout.LayoutParams(MATCH, MATCH))
        rb.addView(
            IconView(a, if (pdf) K.DOC else K.IMAGE, P.iconSoft),
            FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER)
        )
        left.addView(rb, lp(dp(72), dp(72)))
        val lc = LinearLayout(a)
        lc.orientation = LinearLayout.VERTICAL
        lc.setPadding(dp(10), 0, 0, 0)
        lc.addView(a.tv(tr("File Size Reduced"), 12.5f, P.text, 1))
        redPct = a.tv("0%", 30f, 0xFF19C98A.toInt(), 2)
        redPct.post {
            val w = redPct.paint.measureText("00%")
            redPct.paint.shader = LinearGradient(
                0f, 0f, w, 0f, 0xFF19E39A.toInt(), 0xFF0FB57E.toInt(), Shader.TileMode.CLAMP
            )
            redPct.invalidate()
        }
        lc.addView(redPct)
        fromTo = a.tv("", 11.5f, P.sub)
        fromTo.maxLines = 2
        lc.addView(fromTo)
        left.addView(lc, lp(0, WRAP, 1f))
        card.addView(left, lp(0, WRAP, 1.15f))

        val dv = View(a)
        dv.setBackgroundColor(P.divider)
        val dl = lp(dp(1), dp(58))
        dl.setMargins(dp(8), 0, dp(8), 0)
        card.addView(dv, dl)

        // right: Quality Preserved
        val right = LinearLayout(a)
        right.orientation = LinearLayout.HORIZONTAL
        right.gravity = Gravity.CENTER_VERTICAL
        val leaf = FrameLayout(a)
        val ld = GradientDrawable()
        ld.shape = GradientDrawable.OVAL
        ld.setColor(0x2219E39A)
        leaf.background = ld
        leaf.addView(IconView(a, K.LEAF, 0xFF19C98A.toInt()), FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        right.addView(leaf, lp(dp(46), dp(46)))
        val rc = LinearLayout(a)
        rc.orientation = LinearLayout.VERTICAL
        rc.setPadding(dp(10), 0, 0, 0)
        rc.addView(a.tv(tr("Quality Preserved"), 13f, P.text, 2))
        qualSub = a.tv("", 11f, P.sub)
        rc.addView(qualSub)
        right.addView(rc, lp(0, WRAP, 1f))
        card.addView(right, lp(0, WRAP, 1f))
        return card
    }

    // kind 0 = Share (filled), 1 = Save, 2 = Compress Again
    private fun actionBtn(kind: Int, title: String, sub: String, onClick: () -> Unit): Btn {
        val b = LinearLayout(a)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER_VERTICAL
        b.minimumHeight = dp(58)
        b.setPadding(dp(10), dp(8), dp(8), dp(8))
        val filled = kind == 0
        b.background = if (filled) a.grad(P.chipSel, GradientDrawable.Orientation.LEFT_RIGHT, 18)
        else a.rr(P.card, 18, P.cardEdge, 1)

        if (filled) {
            b.addView(Icon2View(a, K2.SHARE, Color.WHITE), lp(dp(24), dp(24)))
        } else {
            val tile = FrameLayout(a)
            val td = GradientDrawable()
            td.shape = GradientDrawable.OVAL
            td.setColor(P.chip)
            tile.background = td
            tile.addView(
                IconView(a, if (kind == 1) K.FOLDER else K.REFRESH, P.iconSoft),
                FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER)
            )
            b.addView(tile, lp(dp(34), dp(34)))
        }
        val col = LinearLayout(a)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(8), 0, 0, 0)
        val t = a.tv(tr(title), 13.5f, if (filled) Color.WHITE else P.text, 2)
        t.maxLines = 2
        val s = a.tv(tr(sub), 10f, if (filled) 0xDDFFFFFF.toInt() else P.sub)
        s.maxLines = 2
        col.addView(t)
        col.addView(s)
        b.addView(col, lp(0, WRAP, 1f))
        b.setOnClickListener { onClick() }
        return Btn(b, t, s)
    }

    private fun backHomeButton(): View {
        val b = LinearLayout(a)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER_VERTICAL
        b.setPadding(dp(18), 0, dp(18), 0)
        b.background = a.grad(P.grad3, GradientDrawable.Orientation.LEFT_RIGHT, 32)
        b.elevation = dp(6).toFloat()
        val circle = FrameLayout(a)
        val cd = GradientDrawable()
        cd.shape = GradientDrawable.OVAL
        cd.setColor(0x44FFFFFF)
        circle.background = cd
        circle.addView(IconView(a, K.HOME, Color.WHITE), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        b.addView(circle, lp(dp(40), dp(40)))
        val col = LinearLayout(a)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(14), 0, 0, 0)
        col.addView(a.tv(tr("Back to Home"), 17f, Color.WHITE, 2))
        col.addView(a.tv(tr("Go to main screen"), 11.5f, 0xDDFFFFFF.toInt()))
        b.addView(col, lp(0, WRAP, 1f))
        b.setOnClickListener { goHome() }
        return b
    }

    // ---------- processing state ----------

    private fun showProcessing() {
        val first = items[0]
        title1.text = tr("Compressing") + " "
        gradTv(title2, tr(if (pdf) "PDF…" else "Image…"), 0xFF00C8FF.toInt(), 0xFF8A3DFF.toInt())
        subText.text = tr("Please wait while we compress your file.") + "\n" + tr("Large files may take a little more time.")
        cancelTv.visibility = View.VISIBLE
        status.progress = 0f

        origCard.img.setImageBitmap(first.thumb)
        origCard.size.text = sizeText(items.sumOf { it.size })
        origCard.dims.text = dimsText(items)
        compCard.img.setImageBitmap(first.thumb)
        compCard.img.alpha = 0.3f
        compCard.size.text = "\u2026"
        compCard.dims.text = "\u2026"
        compCard.pill?.visibility = View.GONE

        ring.pct = 0f
        redPct.text = "0%"
        fromTo.text = ""
        qualSub.text = ""
        setButtonsEnabled(false)
    }

    private fun setButtonsEnabled(on: Boolean) {
        val al = if (on) 1f else 0.45f
        shareBtn.root.alpha = al
        saveBtn.root.alpha = al
        againBtn.root.alpha = al
    }

    // ---------- the work ----------

    private fun startThread() {
        val its = ArrayList(items)
        val oc = opts.copy()
        val n = its.size
        val isPdf = pdf
        File(a.cacheDir, "out").deleteRecursively()
        val dir = File(a.cacheDir, "out/r" + System.nanoTime())
        dir.mkdirs()
        Thread {
            var failed = 0
            for (idx in its.indices) {
                if (cancelled) break
                val item = its[idx]
                val cb = { p: Float ->
                    a.runOnUiThread { if (!cancelled) status.progress = (idx + p) / n }
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
            var cp: Bitmap? = null
            var cdims: IntArray? = null
            var cpages = 0
            val firstDone = its.firstOrNull { it.outFile != null }
            if (!cancelled && firstDone != null) {
                val of = firstDone.outFile!!
                cp = Engine.previewOf(of, isPdf)
                if (isPdf) cpages = Engine.pageCountOf(of) else cdims = Engine.dimsOf(of)
            }
            a.runOnUiThread {
                if (!cancelled && a.screen == "result") finishRun(its, cp, cdims, cpages)
            }
        }.start()
    }

    private fun finishRun(its: List<Item>, cp: Bitmap?, cdims: IntArray?, cpages: Int) {
        running = false
        val ok = its.filter { it.outFile != null }
        if (ok.isEmpty()) {
            a.toast(tr("Compression failed"))
            a.cui.reshow()
            return
        }
        done = true
        outItems = ok

        var orig = 0L
        var out = 0L
        for (i in ok) {
            orig += i.size
            out += i.outFile!!.length()
        }
        val pct = if (orig > 0) ((orig - out) * 100.0 / orig).toInt().coerceIn(0, 99) else 0

        // the circle turns into the tick
        status.progress = 1f
        status.finish()
        title1.text = tr("Compression") + " "
        gradTv(title2, tr("Completed!"), 0xFF00C8FF.toInt(), 0xFF8A3DFF.toInt())
        subText.text = tr(
            if (ok.size > 1) "Your files have been compressed successfully."
            else if (pdf) "Your PDF has been compressed successfully."
            else "Your image has been compressed successfully."
        ) + "\n" + tr("You can now save, share or compress another file.")
        cancelTv.visibility = View.GONE

        // pictures and sizes
        origCard.img.setImageBitmap(ok[0].thumb)
        origCard.size.text = sizeText(orig)
        origCard.dims.text = dimsText(ok)
        compCard.img.setImageBitmap(cp ?: ok[0].thumb)
        compCard.img.alpha = 1f
        compCard.size.text = sizeText(out)
        compCard.dims.text = if (ok.size > 1) ok.size.toString() + " " + tr("files")
        else if (pdf) cpages.toString() + " " + tr("pages")
        else if (cdims != null) "${cdims[0]} \u00D7 ${cdims[1]}" else dimsText(ok)
        val pill = compCard.pill
        if (pill != null) {
            pill.text = "\u2193 $pct%"
            pill.visibility = View.VISIBLE
        }

        // the stats card
        fromTo.text = sizeText(orig) + "  \u2192  " + sizeText(out)
        qualSub.text = tr(
            if (pct == 0) "This file was already well optimized."
            else if (pdf) "No noticeable loss in readability."
            else "No noticeable loss in image quality."
        )
        val an = ValueAnimator.ofFloat(0f, pct.toFloat())
        an.duration = 900
        an.addUpdateListener {
            val v = it.animatedValue as Float
            ring.pct = v
            redPct.text = v.toInt().toString() + "%"
        }
        an.start()

        setButtonsEnabled(true)
    }

    // ---------- buttons ----------

    private fun doShare() {
        if (!done) return
        try {
            Engine.share(a, outItems.map { it.outFile!! })
        } catch (e: Throwable) {
            a.toast(tr("Could not share"))
        }
    }

    private fun doSave() {
        if (!done) return
        if (saved) {
            a.toast(tr("Already saved"))
            return
        }
        var okCount = 0
        var where = ""
        for (i in outItems) {
            val r = Engine.saveToDevice(a, i.outFile!!, Prefs.storage())
            if (r != null) {
                okCount++
                where = r
            }
        }
        if (okCount > 0) {
            saved = true
            saveBtn.title.text = tr("Saved!")
            saveBtn.sub.text = where
            a.toast(tr("Saved to") + " " + where)
        } else {
            a.toast(tr("Could not save"))
        }
    }

    private fun doAgain() {
        if (!done) return
        a.pickFiles(pdf)
    }
}
