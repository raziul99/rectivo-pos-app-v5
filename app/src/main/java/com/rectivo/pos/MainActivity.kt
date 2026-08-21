package com.rectivo.pos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var splash: View
    private var splashHidden = false
    private var fcmToken: String? = null

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val uris = if (result.resultCode == RESULT_OK && data != null) {
                data.data?.let { arrayOf(it) }
            } else null
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipe = findViewById(R.id.swipe)

        // Splash: full logo + spinner, shown until the first page loads
        splash = findViewById(R.id.splash)
        findViewById<ImageView>(R.id.splashLogo)
            .startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        // Safety: never let the splash stick if a load stalls
        splash.postDelayed({ hideSplash() }, 8000)

        askNotificationPermission()

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                // Keep POS pages inside the app; send external links (tel/mail/whatsapp) out
                return if (url.startsWith(BASE_URL) ||
                    url.startsWith("https://pos.rectivo.com")
                ) {
                    false
                } else {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipe.isRefreshing = false
                hideSplash()
                registerTokenWithWebSession()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = params?.createIntent()
                return try {
                    fileChooser.launch(intent)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }

        swipe.setOnRefreshListener { webView.reload() }

        // Grab the FCM token; will be injected once a POS page has loaded
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                registerTokenWithWebSession()
            }
        }

        // Open the URL from a tapped notification, else the POS home
        val startUrl = intent?.getStringExtra(EXTRA_URL) ?: BASE_URL
        if (savedInstanceState == null) webView.loadUrl(startUrl)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(EXTRA_URL)?.let { webView.loadUrl(it) }
    }

    /**
     * Injects a tiny script into the loaded POS page that POSTs the FCM token
     * to /fcm/register using the page's own logged-in session cookie, so the
     * token is stored against the correct user. No changes to your Blade views
     * are required.
     */
    private fun registerTokenWithWebSession() {
        val token = fcmToken ?: return
        val js = """
            (function(){
              try{
                var t='$token';
                if(!t) return;
                if(window.__rectivoTokenSent===t) return;
                fetch('/fcm/register',{
                  method:'POST',
                  credentials:'same-origin',
                  headers:{'Content-Type':'application/json','X-Requested-With':'XMLHttpRequest'},
                  body:JSON.stringify({token:t,platform:'android'})
                }).then(function(){window.__rectivoTokenSent=t;}).catch(function(){});
              }catch(e){}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun hideSplash() {
        if (splashHidden) return
        splashHidden = true
        splash.animate().alpha(0f).setDuration(350)
            .withEndAction { splash.visibility = View.GONE }.start()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        // Change this if your POS domain changes.
        const val BASE_URL = "https://pos.rectivo.com/"
        const val EXTRA_URL = "target_url"
    }
}
