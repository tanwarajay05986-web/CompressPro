package com.akay.compresspro

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.toast(m: String) {
    Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}

fun withAlpha(c: Int, a: Int): Int = (a.coerceIn(0, 255) shl 24) or (c and 0x00FFFFFF)

// colours (they change with the Dark / Light theme)
object P {
    var dark = true
    val bg: Int get() = if (dark) 0xFF040A18.toInt() else 0xFFEFF3FB.toInt()
    val card: Int get() = if (dark) 0xFF0B1730.toInt() else 0xFFFFFFFF.toInt()
    val cardEdge: Int get() = if (dark) 0xFF1B3560.toInt() else 0xFFD3DDEE.toInt()
    val divider: Int get() = if (dark) 0xFF18284A.toInt() else 0xFFE3E9F4.toInt()
    val text: Int get() = if (dark) 0xFFFFFFFF.toInt() else 0xFF0D1A33.toInt()
    val sub: Int get() = if (dark) 0xFFB4C6E6.toInt() else 0xFF5A6B8C.toInt()
    val navBg: Int get() = if (dark) 0xFF070E20.toInt() else 0xFFFFFFFF.toInt()
    val navOff: Int get() = if (dark) 0xFF4A6090.toInt() else 0xFF8A99B8.toInt()
    val iconSoft: Int get() = if (dark) 0xFF8CCBFF.toInt() else 0xFF2F86E0.toInt()
    val blue = 0xFF1E90FF.toInt()
    val cyan = 0xFF00C8FF.toInt()
    val purple = 0xFFA855F7.toInt()
    val gold = 0xFFFFC21A.toInt()
}

// English text is used everywhere in the code; Hindi is looked up here
object T {
    var hi = false

    private val H: Map<String, String> = mapOf(
        "Home" to "होम",
        "Settings" to "सेटिंग्स",
        "Smaller Size" to "छोटा साइज़",
        "Same Quality" to "वही क्वालिटी",
        "Make Your Files" to "अपनी फ़ाइलें बनाएँ",
        "Smaller" to "छोटी",
        "Lighter" to "हल्की",
        "Compress and resize your images and PDFs in just a few taps." to
            "अपनी इमेज और PDF को बस कुछ टैप में कंप्रेस और रीसाइज़ करें।",
        "Image" to "इमेज",
        "PDF" to "PDF",
        "Compress / Resize" to "कंप्रेस / रीसाइज़",
        "Reduce image size and adjust dimensions without losing quality." to
            "क्वालिटी खोए बिना इमेज का साइज़ घटाएँ और डाइमेंशन बदलें।",
        "Make your PDF files smaller and fit your needs." to
            "अपनी PDF फ़ाइलों को छोटा करें और ज़रूरत के हिसाब से बनाएँ।",
        "App Theme" to "ऐप थीम",
        "Choose your preferred theme" to "अपनी पसंद की थीम चुनें",
        "Dark" to "डार्क",
        "Light" to "लाइट",
        "Language" to "भाषा",
        "Select app language" to "ऐप की भाषा चुनें",
        "Output Quality" to "आउटपुट क्वालिटी",
        "Set default quality for compression" to "कंप्रेशन की डिफ़ॉल्ट क्वालिटी सेट करें",
        "Low" to "कम",
        "Medium" to "मध्यम",
        "High" to "ज़्यादा",
        "Storage Location" to "स्टोरेज लोकेशन",
        "Choose where to save files" to "फ़ाइलें कहाँ सेव हों, चुनें",
        "Internal" to "इंटरनल",
        "Downloads" to "डाउनलोड्स",
        "About App" to "ऐप के बारे में",
        "Version, developer and more" to "वर्ज़न, डेवलपर और बहुत कुछ",
        "Privacy Policy" to "प्राइवेसी पॉलिसी",
        "Rate Us" to "हमें रेट करें",
        "Support us with your rating" to "अपनी रेटिंग से हमारा साथ दें",
        "Cancel" to "रद्द करें",
        "Close" to "बंद करें",
        "Free app, no subscription." to "फ़्री ऐप, कोई सब्सक्रिप्शन नहीं।",
        "Made by Akay" to "Akay ने बनाया",
        "Permission needed" to "अनुमति ज़रूरी है",
        "CompressPro needs access to your photos to pick images and PDFs. Without this permission the app cannot work." to
            "इमेज और PDF चुनने के लिए CompressPro को आपकी फ़ोटो का एक्सेस चाहिए। इस अनुमति के बिना ऐप काम नहीं करेगा।",
        "Allow access" to "एक्सेस दें",
        "Open Settings" to "सेटिंग्स खोलें",
        "Coming in the next stage" to "अगले स्टेज में आएगा",
        "CompressPro is 100% free. No subscription!" to "CompressPro 100% फ़्री है। कोई सब्सक्रिप्शन नहीं!",
        "Privacy policy link will be added soon" to "प्राइवेसी पॉलिसी का लिंक जल्द जुड़ेगा",
        "Could not open Play Store" to "Play Store नहीं खुल पाया"
    )

    fun t(s: String): String = if (hi) (H[s] ?: s) else s
}

// saved settings
object Prefs {
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        if (sp == null) sp = c.applicationContext.getSharedPreferences("compresspro", Context.MODE_PRIVATE)
        P.dark = dark()
        T.hi = lang() == 1
    }

    fun dark(): Boolean = sp!!.getBoolean("dark", true)

    fun setDark(v: Boolean) {
        sp!!.edit().putBoolean("dark", v).apply()
        P.dark = v
    }

    // 0 = English, 1 = Hindi
    fun lang(): Int = sp!!.getInt("lang", 0)

    fun setLang(v: Int) {
        sp!!.edit().putInt("lang", v).apply()
        T.hi = v == 1
    }

    // 0 = Low (30), 1 = Medium (60), 2 = High (90)
    fun quality(): Int = sp!!.getInt("quality", 1)

    fun setQuality(v: Int) {
        sp!!.edit().putInt("quality", v).apply()
    }

    // 0 = Internal (Pictures / Downloads), 1 = Downloads
    fun storage(): Int = sp!!.getInt("storage", 0)

    fun setStorage(v: Int) {
        sp!!.edit().putInt("storage", v).apply()
    }

    fun asked(): Boolean = sp!!.getBoolean("asked", false)

    fun setAsked(v: Boolean) {
        sp!!.edit().putBoolean("asked", v).apply()
    }
}
