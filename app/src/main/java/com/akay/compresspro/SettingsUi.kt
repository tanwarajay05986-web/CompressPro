package com.akay.compresspro

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
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

    // in the white theme every icon sits in a small white tile
    val tile = FrameLayout(this)
    if (!P.dark) {
        tile.background = rr(P.tile, 12, P.cardEdge, 1)
        tile.elevation = dp(2).toFloat()
    }
    val isz = if (P.dark) 34 else 24
    tile.addView(IconView(this, icon, P.iconSoft), FrameLayout.LayoutParams(dp(isz), dp(isz), Gravity.CENTER))
    r.addView(tile, lp(dp(40), dp(40)))

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
        box.addView(dv, lpm(MATCH, 1, 56, 0, 0, 0))
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

// shows the chosen Google account
private fun MainActivity.accountCard(email: String): View {
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    r.gravity = Gravity.CENTER_VERTICAL
    r.background = rr(P.card, 22, P.cardEdge, 1)
    r.setPadding(dp(14), dp(14), dp(14), dp(14))

    val av = FrameLayout(this)
    val bd = GradientDrawable(GradientDrawable.Orientation.TL_BR, P.grad3)
    bd.shape = GradientDrawable.OVAL
    av.background = bd
    val ini = tv(email.take(1).uppercase(), 20f, Color.WHITE, 2)
    ini.gravity = Gravity.CENTER
    av.addView(ini, FrameLayout.LayoutParams(MATCH, MATCH))
    r.addView(av, lp(dp(46), dp(46)))

    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.setPadding(dp(14), 0, 0, 0)
    col.addView(tv(tr("Signed in as"), 12f, P.sub))
    val em = tv(email, 15f, P.text, 1)
    em.maxLines = 1
    em.ellipsize = TextUtils.TruncateAt.END
    col.addView(em)
    r.addView(col, lp(0, WRAP, 1f))
    return r
}

private fun MainActivity.confirmLogout() {
    AlertDialog.Builder(this)
        .setTitle(tr("Logout"))
        .setMessage(tr("Do you want to logout?"))
        .setPositiveButton(tr("Logout")) { _, _ ->
            Prefs.setEmail("")
            toast(tr("Logged out"))
            showSettings()
        }
        .setNegativeButton(tr("Cancel"), null)
        .show()
}

private fun MainActivity.logoutButton(): View {
    val red = if (P.dark) 0xFFFF4D6D.toInt() else 0xFFE8245B.toInt()
    val b = LinearLayout(this)
    b.orientation = LinearLayout.HORIZONTAL
    b.gravity = Gravity.CENTER
    b.background = if (P.dark) rr(0xFF2A0F1E.toInt(), 28, 0xFF5A1D32.toInt(), 1) else rr(0xFFFFE3EA.toInt(), 28)
    b.addView(IconView(this, K.LOGOUT, red), lp(dp(24), dp(24)))
    val t = tv(tr("Logout"), 17f, red, 2)
    t.setPadding(dp(12), 0, 0, 0)
    b.addView(t)
    b.setOnClickListener { confirmLogout() }
    return b
}

private fun MainActivity.loginButton(): View {
    val blue = 0xFF2F7DFF.toInt()
    val b = LinearLayout(this)
    b.orientation = LinearLayout.HORIZONTAL
    b.gravity = Gravity.CENTER
    b.background = if (P.dark) rr(0xFF0E2447.toInt(), 28, 0xFF1B3560.toInt(), 1) else rr(0xFFE3EEFF.toInt(), 28)
    b.addView(GLogo(this), lp(dp(22), dp(22)))
    val t = tv(tr("Login"), 17f, blue, 2)
    t.setPadding(dp(12), 0, 0, 0)
    b.addView(t)
    b.setOnClickListener { showLogin("settings") }
    return b
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

    val email = Prefs.email()
    if (email.isNotEmpty()) c.addView(accountCard(email), lpm(MATCH, WRAP, 0, 14, 0, 0))

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

    // login or logout
    val acc = if (email.isNotEmpty()) logoutButton() else loginButton()
    c.addView(acc, lpm(MATCH, 56, 0, 16, 0, 0))

    val sv = ScrollView(this)
    sv.isVerticalScrollBarEnabled = false
    sv.addView(c)

    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.addView(sv, lp(MATCH, 0, 1f))
    root.addView(navBar(1), lp(MATCH, WRAP))
    setScreen(root, P.navBg)
}
