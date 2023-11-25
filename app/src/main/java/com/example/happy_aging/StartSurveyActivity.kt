package com.example.happy_aging

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
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
    private lateinit var userName: String
    private lateinit var spinnerBigCity: Spinner
    private lateinit var spinnerSmallCity: Spinner
    private var selectedBigCity: String? = null
    private var selectedSmallCity: String? = null


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
        spinnerBigCity = findViewById(R.id.spinnerBigCity)
        spinnerSmallCity = findViewById(R.id.spinnerSmallCity)
        findViewById<Button>(R.id.buttonNext).setOnClickListener { handleNextButtonClick() }

        setupSpinners()
    }
    private fun getCityData(): JSONObject {
        val fileName = "cities.json" // `assets` 폴더 내의 파일 이름
        val assets = this.assets
        val inputStream = assets.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)

        inputStream.read(buffer)
        inputStream.close()

        val jsonStr = String(buffer, Charsets.UTF_8)
        return JSONObject(jsonStr)
    }

    private fun setupSpinners() {
        val cityData = getCityData()
        val bigCities = mutableListOf("도/시")
        bigCities.addAll(cityData.keys().asSequence().toList())

        spinnerBigCity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bigCities)
        spinnerBigCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position > 0) { // '도/시'가 아닌 실제 아이템이 선택된 경우
                    selectedBigCity = bigCities[position]
                    val smallCitiesArray = cityData.getJSONArray(selectedBigCity)
                    val smallCitiesList = convertJsonArrayToList(smallCitiesArray)
                    smallCitiesList.add(0, "시/군/구")
                    spinnerSmallCity.adapter = ArrayAdapter(this@StartSurveyActivity, android.R.layout.simple_spinner_dropdown_item, smallCitiesList)

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때의 로직
            }
        }

        // 초기 smallCity 스피너 설정
        spinnerSmallCity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("시/군/구"))

        spinnerSmallCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position > 0) { // '시/군/구'가 아닌 실제 아이템이 선택된 경우
                    selectedSmallCity = parent.getItemAtPosition(position).toString()
                } else {
                    selectedSmallCity = null // 플레이스홀더가 선택된 경우
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSmallCity = null
            }
        }
    }

    private fun convertJsonArrayToList(jsonArray: JSONArray): MutableList<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    private fun handleNextButtonClick() {
        userName = nameEditText.text.toString()
        val address = if (!selectedBigCity.isNullOrBlank() && !selectedSmallCity.isNullOrBlank()) {
            "$selectedBigCity, $selectedSmallCity"
        } else {
            ""
        }

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
        finish()
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

