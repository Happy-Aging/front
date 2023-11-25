package com.example.happy_aging

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onAddressSelected(address: String) {
                val resultIntent = Intent()
                resultIntent.putExtra("address", address)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }, "Android")
        webView.loadUrl("file:///android_asset/daum_address_search.html")


        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                val newWebView = WebView(this@AddressSearchActivity)
                newWebView.webViewClient = WebViewClient()
                newWebView.webChromeClient = this

                val dialog = Dialog(this@AddressSearchActivity)
                dialog.setContentView(newWebView)
                dialog.show()

                newWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        dialog.dismiss()
                    }
                }

                (resultMsg.obj as WebView.WebViewTransport).webView = newWebView
                resultMsg.sendToTarget()

                return true
            }
        }



    }
}
