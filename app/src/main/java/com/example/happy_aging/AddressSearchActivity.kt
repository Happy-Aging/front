package com.example.happy_aging

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class AddressSearchActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_search)

        webView = findViewById(R.id.webView)
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // DOM Storage 활성화
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d("WebViewLoad", "Page finished loading: $url")
                view.loadUrl("javascript:sample2_execDaumPostcode();")
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                Log.e("WebViewError", "Error loading page: ${error.description}")
            }
        }

        webView.loadUrl("file:///android_asset/daum_address_search.html")
    }

    private inner class WebAppInterface {
        @JavascriptInterface
        fun processDATA(data: String) {
            Intent().apply {
                putExtra("address", data)
                setResult(RESULT_OK, this)
                finish()
            }
        }
    }
}
