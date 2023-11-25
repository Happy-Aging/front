package com.example.happy_aging

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
class StartSurveyActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var userName: String
    private lateinit var addressResultLauncher: ActivityResultLauncher<Intent>


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

        addressResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val address = result.data?.getStringExtra("address")
                addressEditText.setText(address)
            }
        }
    }


    private fun setupToolbar() {
        val customToolbar: View = layoutInflater.inflate(R.layout.toolbar_title, null)
        customToolbar.findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        val toolbarTitle: TextView = customToolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.apply {
            text = "시니어 등록"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(typeface, Typeface.BOLD)
        }

        findViewById<Toolbar>(R.id.toolbar).apply {
            addView(customToolbar)
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    private fun initializeViews() {
        nameEditText = findViewById(R.id.editTextName)
        addressEditText = findViewById(R.id.editTextBigCity)
        findViewById<Button>(R.id.buttonNext).setOnClickListener { handleNextButtonClick() }

        addressEditText.setOnClickListener {
            Log.d("SurveyStartResults", "AddressSearchActivity를 여는 인텐트 생성 및 실행")
            val intent = Intent(this, AddressSearchActivity::class.java)
            addressResultLauncher.launch(intent)
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
            return true
        }
        return super.onOptionsItemSelected(item)
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
        private const val ADDRESS_SEARCH_REQUEST_CODE = 1

        private fun String.toRequestBody() =
            okhttp3.RequestBody.create(MediaType.parse("application/json"), this)
    }
}

