package com.mohammad.rejimat

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * يعرض التطبيق من ملفات assets عبر WebViewAssetLoader.
 *
 * لماذا لا نستعمل file:// مباشرة:
 * ملفات file:// تحصل على أصل (origin) غير ثابت، فتضيع بيانات localStorage
 * عند تحديث التطبيق. AssetLoader يقدّم الصفحة من أصل https ثابت،
 * فتبقى بيانات الوزن والالتزام محفوظة بين التحديثات.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    companion object {
        private const val HOME = "https://appassets.androidplatform.net/assets/index.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(backgroundForTheme())
            overScrollMode = WebView.OVER_SCROLL_NEVER
        }
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // بدونها لا يُحفظ شيء
            loadWithOverviewMode = true
            useWideViewPort = false
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = true
            // علامة تعرّف الصفحة على أنها داخل التطبيق
            userAgentString = "$userAgentString RejimatAndroid"
        }

        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val url = request.url
                // الروابط الداخلية تبقى داخل التطبيق، والخارجية تُفتح في المتصفح
                if (url.host == "appassets.androidplatform.net") return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())))
                    true
                } catch (_: Exception) {
                    true
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // إغلاق أي نافذة مفتوحة قبل الخروج من التطبيق
                web.evaluateJavascript(
                    """(function(){
                         var m=document.querySelector('.modal.show, .modal.open');
                         if(m){ m.classList.remove('show'); m.classList.remove('open'); return 'closed'; }
                         return 'exit';
                       })();"""
                ) { result ->
                    if (result.contains("exit")) {
                        if (web.canGoBack()) web.goBack() else finish()
                    }
                }
            }
        })

        if (savedInstanceState == null) web.loadUrl(HOME)
    }

    private fun backgroundForTheme(): Int {
        val night = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (night) 0xFF0B1017.toInt() else 0xFFF4F6F8.toInt()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        web.restoreState(savedInstanceState)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
