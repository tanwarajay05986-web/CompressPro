package com.akay.compresspro

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// starts the ads when the app starts
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Ads.init(this)
    }
}

// AdMob. These are Google TEST ads (safe to click). Put your real ad unit before publishing.
object Ads {
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    // set to false to turn all ads off
    const val ENABLED = true

    // minimum time between two ads. 0 = an ad on every click (good for testing).
    // Before publishing use 60000L (one minute).
    const val GAP_MS = 0L

    private val main = Handler(Looper.getMainLooper())
    private var app: Application? = null
    private var inter: InterstitialAd? = null
    private var loading = false
    private var started = false
    private var lastShown = 0L
    private var showing = false

    fun init(a: Application) {
        if (!ENABLED || started) return
        started = true
        app = a
        Thread {
            try {
                MobileAds.initialize(a) { main.post { load() } }
            } catch (e: Throwable) {
            }
        }.start()
    }

    private fun load() {
        val a = app ?: return
        if (!ENABLED || loading || inter != null) return
        loading = true
        try {
            InterstitialAd.load(
                a, INTERSTITIAL_ID, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        inter = ad
                        loading = false
                    }

                    override fun onAdFailedToLoad(err: LoadAdError) {
                        inter = null
                        loading = false
                    }
                }
            )
        } catch (e: Throwable) {
            loading = false
        }
    }

    // shows an ad (if one is ready) and then runs `then`. If there is no ad, `then` runs at once.
    fun show(act: Activity, then: () -> Unit) {
        val ad = inter
        val now = System.currentTimeMillis()
        if (!ENABLED || showing || ad == null || now - lastShown < GAP_MS) {
            if (ad == null) load()
            then()
            return
        }
        inter = null
        showing = true
        var done = false
        fun finish() {
            if (done) return
            done = true
            showing = false
            lastShown = System.currentTimeMillis()
            load()
            then()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                finish()
            }
        }
        try {
            ad.show(act)
        } catch (e: Throwable) {
            finish()
        }
    }
}
