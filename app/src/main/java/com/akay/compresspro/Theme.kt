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
    var dark = false
    val bg: Int get() = if (dark) 0xFF040A18.toInt() else 0xFFF3F6FD.toInt()
    val card: Int get() = if (dark) 0xFF0B1730.toInt() else 0xFFFFFFFF.toInt()
    val cardEdge: Int get() = if (dark) 0xFF1B3560.toInt() else 0xFFE2E9F6.toInt()
    val divider: Int get() = if (dark) 0xFF18284A.toInt() else 0xFFE8EDF7.toInt()
    val text: Int get() = if (dark) 0xFFFFFFFF.toInt() else 0xFF0F1B3D.toInt()
    val sub: Int get() = if (dark) 0xFFB4C6E6.toInt() else 0xFF5F6F90.toInt()
    val navBg: Int get() = if (dark) 0xFF070E20.toInt() else 0xFFFFFFFF.toInt()
    val navOff: Int get() = if (dark) 0xFF4A6090.toInt() else 0xFF8C9AB8.toInt()
    val iconSoft: Int get() = if (dark) 0xFF8CCBFF.toInt() else 0xFF2F86E0.toInt()
    val chip: Int get() = if (dark) 0xFF0F2347.toInt() else 0xFFEBF1FB.toInt()
    val field: Int get() = if (dark) 0xFF081832.toInt() else 0xFFF5F8FD.toInt()
    val tile: Int get() = if (dark) 0x00000000 else 0xFFFFFFFF.toInt()
    val chipSel: IntArray get() = intArrayOf(0xFF1E8BFF.toInt(), 0xFF7A3DFF.toInt())
    val grad3: IntArray get() = intArrayOf(0xFF12C2FF.toInt(), 0xFF3B5BFF.toInt(), 0xFF8A3DFF.toInt())
    val blue = 0xFF1E90FF.toInt()
    val cyan = 0xFF00C8FF.toInt()
    val purple = 0xFFA855F7.toInt()
    val gold = 0xFFFFC21A.toInt()
    val green = 0xFF19E39A.toInt()
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
        "Light" to "व्हाइट",
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
        "CompressPro is 100% free. No subscription!" to "CompressPro 100% फ़्री है। कोई सब्सक्रिप्शन नहीं!",
        "Privacy policy link will be added soon" to "प्राइवेसी पॉलिसी का लिंक जल्द जुड़ेगा",
        "Could not open Play Store" to "Play Store नहीं खुल पाया",
        "Compression Settings" to "कंप्रेशन सेटिंग्स",
        "Adjust settings to get the best balance between quality and file size." to
            "क्वालिटी और फ़ाइल साइज़ के बीच सबसे अच्छा संतुलन पाने के लिए सेटिंग्स बदलें।",
        "ESTIMATED SIZE" to "अनुमानित साइज़",
        "(After Compression)" to "(कंप्रेशन के बाद)",
        "Saved" to "बचत",
        "Original:" to "ओरिजिनल:",
        "Quality" to "क्वालिटी",
        "Adjust the compression quality." to "कंप्रेशन की क्वालिटी बदलें।",
        "Low (30%)" to "कम (30%)",
        "Medium (60%)" to "मध्यम (60%)",
        "High (90%)" to "ज़्यादा (90%)",
        "Dimensions" to "डाइमेंशन",
        "Set your desired width and height." to "अपनी पसंद की चौड़ाई और ऊँचाई सेट करें।",
        "Height" to "ऊँचाई",
        "Width" to "चौड़ाई",
        "Lock" to "लॉक",
        "Aspect Ratio" to "एस्पेक्ट रेशियो",
        "Custom" to "कस्टम",
        "Format" to "फ़ॉर्मेट",
        "Choose output format for your image." to "अपनी इमेज का आउटपुट फ़ॉर्मेट चुनें।",
        "Original" to "ओरिजिनल",
        "Compress Image" to "इमेज कंप्रेस करें",
        "Compress PDF" to "PDF कंप्रेस करें",
        "Tap to start compression" to "कंप्रेशन शुरू करने के लिए टैप करें",
        "Loading…" to "लोड हो रहा है…",
        "Could not open the selected files" to "चुनी हुई फ़ाइलें खुल नहीं पाईं",
        "Save" to "सेव करें",
        "Share" to "शेयर करें",
        "Saved to" to "सेव हुआ:",
        "Could not save" to "सेव नहीं हो पाया",
        "Cancelled" to "रद्द किया गया",
        "Compression failed" to "कंप्रेशन नहीं हो पाया",
        "Could not share" to "शेयर नहीं हो पाया",
        "Special 200 KB" to "स्पेशल 200 KB",
        "Auto" to "ऑटो",
        "Special mode ON: about 200 KB file. Only dimensions can be changed." to
            "स्पेशल मोड चालू: फ़ाइल 200 KB से कम की बनेगी। सिर्फ़ डाइमेंशन बदल सकते हैं।",
        "Special mode OFF" to "स्पेशल मोड बंद",
        "Compress Your Images" to "अपनी इमेज",
        "& PDFs" to "और PDF",
        "Effortlessly" to "आसानी से कंप्रेस करें",
        "Save space, share faster, and keep your files in perfect quality." to
            "जगह बचाएँ, तेज़ी से शेयर करें और फ़ाइलों की क्वालिटी बनाए रखें।",
        "Fast Compression" to "तेज़ कंप्रेशन",
        "High Quality Output" to "हाई क्वालिटी आउटपुट",
        "Easy to Share" to "शेयर करना आसान",
        "Secure & Private" to "सुरक्षित और प्राइवेट",
        "Continue with Google" to "Google से जारी रखें",
        "Skip" to "छोड़ें",
        "You can always sign in later from settings." to "आप बाद में सेटिंग्स से भी लॉगिन कर सकते हैं।",
        "Login" to "लॉगिन",
        "Logout" to "लॉगआउट",
        "Signed in as" to "लॉगिन:",
        "Do you want to logout?" to "क्या आप लॉगआउट करना चाहते हैं?",
        "No Google account was chosen" to "कोई Google अकाउंट नहीं चुना गया",
        "Could not open account picker" to "अकाउंट पिकर नहीं खुला",
        "Logged out" to "लॉगआउट हो गया",
        "Compression" to "कंप्रेशन",
        "Completed!" to "पूरा हुआ!",
        "Compressing" to "कंप्रेस हो रहा है",
        "Image…" to "इमेज…",
        "PDF…" to "PDF…",
        "Please wait while we compress your file." to "कृपया इंतज़ार करें, फ़ाइल कंप्रेस हो रही है।",
        "Large files may take a little more time." to "बड़ी फ़ाइलों में थोड़ा ज़्यादा समय लग सकता है।",
        "Your image has been compressed successfully." to "आपकी इमेज सफलतापूर्वक कंप्रेस हो गई।",
        "Your PDF has been compressed successfully." to "आपकी PDF सफलतापूर्वक कंप्रेस हो गई।",
        "Your files have been compressed successfully." to "आपकी फ़ाइलें सफलतापूर्वक कंप्रेस हो गईं।",
        "You can now save, share or compress another file." to "अब आप सेव, शेयर या दूसरी फ़ाइल कंप्रेस कर सकते हैं।",
        "Compressed" to "कंप्रेस्ड",
        "files" to "फ़ाइलें",
        "pages" to "पेज",
        "File Size Reduced" to "फ़ाइल साइज़ घटा",
        "Quality Preserved" to "क्वालिटी बरकरार",
        "No noticeable loss in image quality." to "इमेज क्वालिटी में कोई ख़ास फ़र्क़ नहीं।",
        "No noticeable loss in readability." to "पढ़ने में कोई ख़ास फ़र्क़ नहीं।",
        "This file was already well optimized." to "यह फ़ाइल पहले से ही अच्छी तरह छोटी थी।",
        "Share your compressed file" to "अपनी कंप्रेस्ड फ़ाइल शेयर करें",
        "Save to device" to "फ़ोन में सेव करें",
        "Saved!" to "सेव हो गया!",
        "Already saved" to "पहले ही सेव हो चुका है",
        "Compress Again" to "फिर कंप्रेस करें",
        "Start a new process" to "नई प्रक्रिया शुरू करें",
        "Back to Home" to "होम पर वापस",
        "Go to main screen" to "मुख्य स्क्रीन पर जाएँ"
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

    fun dark(): Boolean = sp!!.getBoolean("dark", false)

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

    // login page was shown once (after Continue or Skip)
    fun loginSeen(): Boolean = sp!!.getBoolean("login_seen", false)

    fun setLoginSeen(v: Boolean) {
        sp!!.edit().putBoolean("login_seen", v).apply()
    }

    // the Google account chosen by the user (empty = not logged in)
    fun email(): String = sp!!.getString("email", "") ?: ""

    fun setEmail(v: String) {
        sp!!.edit().putString("email", v).apply()
    }
}
