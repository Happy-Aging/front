package com.example.happy_aging

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class StartSurveyActivity : AppCompatActivity(), AddressSearchFragment.OnAddressSelectedListener {

    private lateinit var nameEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var userName: String
    private lateinit var webView: WebView


    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://13.125.35.235:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_survey)
        setupToolbar()
        initializeViews()
        setupWebView()

    }

    private fun setupToolbar() {
        val customToolbar: View = layoutInflater.inflate(R.layout.toolbar_title, null)
        customToolbar.findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        val toolbarTitle: TextView = customToolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.apply {
            text = "시니어 등록"
            setTextColor(ContextCompat.getColor(context, android.R.color.black)) // Set text color to black
            setTypeface(typeface, Typeface.BOLD) // Set text style to bold
        }

        findViewById<Toolbar>(R.id.toolbar).apply {
            addView(customToolbar)
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }


    private fun initializeViews() {
        nameEditText = findViewById(R.id.editTextName)
        addressEditText = findViewById(R.id.editTextAddress)
        webView = findViewById(R.id.webView)
        findViewById<Button>(R.id.buttonNext).setOnClickListener { handleNextButtonClick() }

        addressEditText.setOnClickListener {
            Log.d("SurveyStartResults", "addressEditText가 터치됨 -> webView의 상태: $webView.visibility")

            // 웹뷰를 보이게 하고 Daum 주소 검색 페이지 로드
            webView.visibility = View.VISIBLE
            webView.loadUrl("file:///android_asset/daum_postcode.html")
        }
    }
    private fun setupWebView() {
        Log.d("SurveyStartResults", "setupWebView 메서드 실행됨")

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onAddressSelected(address: String) {
                runOnUiThread {
                    addressEditText.setText(address)
                    webView.visibility = View.GONE
                }
            }
        }, "Android")

        // 첫 로딩 시에는 웹뷰를 숨김
        webView.visibility = View.GONE
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                Log.e("WebView", "Error: ${error.description}")
            }

            override fun onPageFinished(view: WebView, url: String) {
                Log.d("WebView", "Page loaded: $url")
            }
        }

    }

    private fun handleNextButtonClick() {
        userName = nameEditText.text.toString()
        val address = addressEditText.text.toString()

        if (userName.isNotBlank() && address.isNotBlank()) {
            sendSeniorData(userName, address)
        } else {
            Toast.makeText(this, "이름과 주소를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSeniorData(name: String, address: String) {
        val jsonObject = JSONObject().apply {
            put("name", name)
            put("address", address)
        }
        val requestBody = jsonObject.toString().toRequestBody()

        apiService.postSeniorData(requestBody).enqueue(object : Callback<SeniorResponse> {
            override fun onResponse(call: Call<SeniorResponse>, response: Response<SeniorResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("SurveyStartResults", "Received ID: ${it.id}")
                        startSurveyActivity(it.id)
                    }
                } else {
                    Toast.makeText(applicationContext, "서버 오류 발생: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SeniorResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun startSurveyActivity(seniorId: Int) {
        val intent = Intent(this, SurveyActivity::class.java).apply {
            putExtra("seniorId", seniorId)
            putExtra("userName", userName)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAddressSelected(address: String) {
        addressEditText.setText(address)
        hideAddressSearchFragment()
    }

    private fun hideAddressSearchFragment() {
        findViewById<FrameLayout>(R.id.fragmentContainer).visibility = View.GONE
        supportFragmentManager.popBackStack()
    }

    // API Service Interface
    interface ApiService {
        @POST("senior")
        fun postSeniorData(@Body requestBody: RequestBody): Call<SeniorResponse>
    }

    // Response Data Class
    data class SeniorResponse(
        val id: Int,
        val name: String?,
        val age: Int?,
        val address: String?,
        val profile: String?,
        val rank: String?
    )

    companion object {
        private fun String.toRequestBody() =
            okhttp3.RequestBody.create(MediaType.parse("application/json"), this)
    }
}
