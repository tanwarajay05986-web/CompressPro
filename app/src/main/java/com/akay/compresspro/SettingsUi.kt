package com.akay.compresspro

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

// put your privacy policy link here later
private const val PRIVACY_URL = ""

private fun MainActivity.cardBox(): LinearLayout {
    val b = LinearLayout(this)
    b.orientation = LinearLayout.VERTICAL
    b.background = rr(P.card, 22, P.cardEdge, 1)
    b.setPadding(dp(16), dp(4), dp(16), dp(4))
    return b
}

private fun MainActivity.row(
    icon: Int, title: String, sub: String?, value: String?, last: Boolean, onClick: () -> Unit
): View {
    val box = LinearLayout(this)
    box.orientation = LinearLayout.VERTICAL
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    r.gravity = Gravity.CENTER_VERTICAL
    r.setPadding(0, dp(14), 0, dp(14))
    r.addView(IconView(this, icon, P.iconSoft), lp(dp(38), dp(38)))
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

private fun MainActivity.choose(title: String, items: Array<String>, sel: Int, onPick: (Int) -> Unit) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setSingleChoiceItems(items, sel) { d, w ->
            d.dismiss()
            onPick(w)
        }
        .setNegativeButton(tr("Cancel"), null)
        .show()
}

private fun MainActivity.showAbout() {
    AlertDialog.Builder(this)
        .setTitle("CompressPro  v1.0.0")
        .setMessage(
            tr("Smaller Size") + "  \u2022  " + tr("Same Quality") + "\n\n" +
                tr("Free app, no subscription.") + "\n" + tr("Made by Akay")
        )
        .setPositiveButton(tr("Close"), null)
        .show()
}

private fun MainActivity.openPrivacy() {
    if (PRIVACY_URL.isEmpty()) {
        toast(tr("Privacy policy link will be added soon"))
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
    } catch (e: Throwable) {
    }
}

private fun MainActivity.rateUs() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: Throwable) {
        try {
            val u = "https://play.google.com/store/apps/details?id=$packageName"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        } catch (e2: Throwable) {
            toast(tr("Could not open Play Store"))
        }
    }
}

fun MainActivity.showSettings() {
    screen = "settings"
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(18), dp(14), dp(18), dp(24))

    // title row
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

    // app card
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

    // main list
    val themeName = tr(if (P.dark) "Dark" else "Light")
    val langName = if (T.hi) "हिन्दी" else "English"
    val qNames = arrayOf(tr("Low"), tr("Medium"), tr("High"))
    val sNames = arrayOf(tr("Internal"), tr("Downloads"))

    val list = cardBox()
    list.addView(
        row(K.PALETTE, tr("App Theme"), tr("Choose your preferred theme"), themeName, false) {
            choose(tr("App Theme"), arrayOf(tr("Dark"), tr("Light")), if (P.dark) 0 else 1) { w ->
                Prefs.setDark(w == 0)
                showSettings()
            }
        }
    )
    list.addView(
        row(K.GLOBE, tr("Language"), tr("Select app language"), langName, false) {
            choose(tr("Language"), arrayOf("English", "हिन्दी"), Prefs.lang()) { w ->
                Prefs.setLang(w)
                showSettings()
            }
        }
    )
    list.addView(
        row(K.SHIELD, tr("Output Quality"), tr("Set default quality for compression"), qNames[Prefs.quality()], false) {
            choose(tr("Output Quality"), qNames, Prefs.quality()) { w ->
                Prefs.setQuality(w)
                showSettings()
            }
        }
    )
    list.addView(
        row(K.FOLDER, tr("Storage Location"), tr("Choose where to save files"), sNames[Prefs.storage()], false) {
            choose(tr("Storage Location"), sNames, Prefs.storage()) { w ->
                Prefs.setStorage(w)
                showSettings()
            }
        }
    )
    list.addView(
        row(K.INFO, tr("About App"), tr("Version, developer and more"), "v1.0.0", true) {
            showAbout()
        }
    )
    c.addView(list, lpm(MATCH, WRAP, 0, 16, 0, 0))

    // privacy and rating
    val list2 = cardBox()
    list2.addView(row(K.DOC, tr("Privacy Policy"), null, null, false) { openPrivacy() })
    list2.addView(row(K.STAR, tr("Rate Us"), tr("Support us with your rating"), null, true) { rateUs() })
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
