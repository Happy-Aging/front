package com.example.happy_aging

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class AddressSearchFragment : Fragment() {

    private var addressListener: OnAddressSelectedListener? = null

    interface OnAddressSelectedListener {
        fun onAddressSelected(address: String)
    }

    fun setAddressListener(listener: OnAddressSelectedListener) {
        addressListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_address_search, container, false)
        val webView: WebView = view.findViewById(R.id.webView)
        // WebView 설정
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebView", "Page loaded: $url")
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d("WebView", "${consoleMessage.message()} -- From line " +
                        "${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }


        // JavaScript 인터페이스 추가
        webView.addJavascriptInterface(WebAppInterface { address ->
            addressListener?.onAddressSelected(address)
        }, "Android")

        webView.loadUrl("file:///android_asset/daum_postcode.html")

        return view
    }


    class WebAppInterface(private val callback: (String) -> Unit) {
        @JavascriptInterface
        fun processAddress(address: String) {
            Log.d("WebView", "Address received: $address")
            callback(address)
        }
    }


}
