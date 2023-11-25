package com.example.happy_aging

import android.app.AlertDialog
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody
import okhttp3.RequestBody

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.app.Dialog
import android.graphics.Color
import android.os.Parcelable
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.io.Serializable


import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.Path
import java.util.concurrent.TimeUnit


@Suppress("IMPLICIT_CAST_TO_ANY")
class SurveyActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewNumber: TextView
    private lateinit var textViewQuestion: TextView
    private lateinit var layoutAnswerOptions: LinearLayout
    private lateinit var buttonNext: Button
    private lateinit var numberPicker: NumberPicker

    private lateinit var questions: JSONArray
    private var currentQuestionIndex = 0
    private val userResponses = mutableMapOf<String, String>() // Use String as the key type
    private var loadingDialog: Dialog? = null


    private var seniorId = 0
    private val apiService: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(600, TimeUnit.SECONDS) // 연결 타임아웃 설정
            .readTimeout(600, TimeUnit.SECONDS) // 읽기 타임아웃 설정
            .writeTimeout(600, TimeUnit.SECONDS) // 쓰기 타임아웃 설정
            .build()

        // Retrofit 인스턴스 생성
        Retrofit.Builder()
            .baseUrl("http://13.125.35.235:8080/") // 기존 baseUrl 사용
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // OkHttpClient 인스턴스 지정
            .build()
            .create(ApiService::class.java)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)
        seniorId = intent.getIntExtra("seniorId", -1)



        progressBar = findViewById(R.id.progressBar)
        textViewNumber = findViewById(R.id.textViewNumber)
        textViewQuestion = findViewById(R.id.textViewQuestion)
        layoutAnswerOptions = findViewById(R.id.layoutAnswerOptions)
        buttonNext = findViewById(R.id.buttonNext)
        numberPicker = findViewById(R.id.numberPicker)
        numberPicker.minValue = 1900
        numberPicker.maxValue = 2050

        setupToolbar()
        loadQuestions()
        displayQuestion(currentQuestionIndex)

        buttonNext.setOnClickListener {
            if (!isAnswerProvided()) {
                Toast.makeText(this, "이 문항을 답변해야 다음으로 넘어갈 수 있습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentQuestionIndex < questions.length()) {
                saveResponse()
                if(currentQuestionIndex == questions.length() - 1) {
                    if (areAllQuestionsAnswered()) {
                        submitResponsesToServer()
                    } else {
                        Toast.makeText(this, "모든 질문에 답해주세요.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    currentQuestionIndex++
                    displayQuestion(currentQuestionIndex)
                }
            }
        }


    }
    private fun isAnswerProvided(): Boolean {

        try {
            val question = questions.getJSONObject(currentQuestionIndex)
            val answerType = question.getString("answerType")

            return when (answerType) {
                "SINGLE_CHOICE" -> {
                    var isRadioButtonChecked = false
                    for (i in 0 until layoutAnswerOptions.childCount) {
                        val child = layoutAnswerOptions.getChildAt(i)
                        if (child is RadioGroup) {
                            isRadioButtonChecked = child.checkedRadioButtonId != -1
                            break // 라디오 그룹이 발견되면 반복문 종료
                        }
                    }
                    isRadioButtonChecked
                }
                "NUMBER" -> {
                    numberPicker.value != numberPicker.minValue // 넘버 피커의 값이 초기값이 아닌지 확인
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e("SurveyActivity", "isAnswerProvided에서 오류 발생: ${e.message}")
            return false // 예외 발생 시 false 반환
        }
    }


    private fun setupToolbar() {
        val customToolbar: View = layoutInflater.inflate(R.layout.toolbar_title_x, null)
        customToolbar.findViewById<ImageView>(R.id.close_button).setOnClickListener {
            // 대화상자 표시
            showExitConfirmationDialog()
        }
        customToolbar.findViewById<TextView>(R.id.toolbar_title).text = "설문조사"

        findViewById<Toolbar>(R.id.survey_toolbar).apply {
            addView(customToolbar)
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    private fun showExitConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("정말 조사를 끝내시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("끝내기") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("계속 하기") { dialog, _ ->
                dialog.cancel()
            }
        val alert = dialogBuilder.create()
        alert.show()

        // 버튼의 텍스트 색상을 main_orange로 변경
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.main_orange))
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.DarkGray))
    }


    private fun loadQuestions() {
        val jsonStr = application.assets.open("questions.json").bufferedReader().use {
            it.readText()
        }
        questions = JSONArray(jsonStr)
        displayQuestion(currentQuestionIndex)
    }

    private fun displayQuestion(index: Int) {
        if (index >= questions.length()) {
            Log.d("SurveyActivity", "질문이 더 이상 없음")
            return
        }
        val question = questions.getJSONObject(index)
        textViewNumber.text = question.getString("questionNumber")
        textViewQuestion.text = question.getString("questionText")
        val answerType = question.getString("answerType")

        layoutAnswerOptions.removeAllViews()
        numberPicker.visibility = View.GONE

        when (answerType) {
            "NUMBER" -> {
                numberPicker.visibility = View.VISIBLE
            }
            "SINGLE_CHOICE" -> addRadioButtons(question.getJSONArray("options"))
        }

        // 진행 상태 업데이트
        progressBar.progress = (index + 1) * 100 / questions.length()
    }

private fun addRadioButtons(options: JSONArray) {
    val radioGroup = RadioGroup(this)
    for (i in 0 until options.length()) {
        val radioButton = RadioButton(this)
        radioButton.text = options.getString(i)
        radioButton.textSize = 18f        // 기본 라디오 버튼 스타일 사용
        radioGroup.addView(radioButton)
    }
    layoutAnswerOptions.addView(radioGroup) // RadioGroup을 layoutAnswerOptions에 추가
}


    private fun saveResponse() {
        val question = questions.getJSONObject(currentQuestionIndex)
        val questionNumber = question.getString("questionNumber")
        val answerType = question.getString("answerType")

        val response: String = when (answerType) {
            "NUMBER" -> numberPicker.value.toString()
            "SINGLE_CHOICE" -> getSelectedRadioButtonResponse()
            else -> ""
        }
        userResponses[questionNumber] = response

    }

    private fun getSelectedRadioButtonResponse(): String {
        val radioGroup = layoutAnswerOptions.getChildAt(0) as RadioGroup
        return findViewById<RadioButton>(radioGroup.checkedRadioButtonId)?.text.toString()
    }

    private fun areAllQuestionsAnswered(): Boolean = userResponses.size == questions.length()


    private fun startResultActivity(response: SurveyResultsResponse) {

        val userName = intent.getStringExtra("userName")
        Log.d("SurveyActivity", "userName은 $userName")

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("surveyResponse", response as Serializable) // response 객체를 Serializable로 캐스팅
            putExtra("resultId", response.resultId.toString())
            putExtra("date", response.date)
            putExtra("rank", response.rank.toString())
            putExtra("summary", response.summary)
            putExtra("userName", userName)
            Log.d("SurveyActivity", "response은 ${response.toString()}")
        }
        startActivity(intent)
    }
    // 서버에 requestBody 보냄

    private fun showLoadingAndSubmit() {
        loadingDialog = Dialog(this).apply {
            setContentView(R.layout.loading_dialog)
            setCancelable(false)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        loadingDialog?.show()
    }
    private fun submitResponsesToServer() {
        Log.d("SurveyActivity", "Submitting responses to server")
        showLoadingAndSubmit()

        if (!areAllQuestionsAnswered()) {
            Toast.makeText(this, "모든 질문에 답해주세요.", Toast.LENGTH_SHORT).show()
            loadingDialog?.dismiss()
            return
        }

        val responseDTOs = userResponses.map { (questionNumber, response) ->
            ResponseDTO(questionNumber, response)
        }
        val surveyResultsRequest = SurveyResultsRequest(responseDTOs)
        val requestBody = Gson().toJson(surveyResultsRequest).toRequestBody()

        apiService.submitSurveyResults(seniorId, requestBody).enqueue(object : Callback<SurveyResultsResponse> {
            override fun onResponse(call: Call<SurveyResultsResponse>, response: Response<SurveyResultsResponse>) {
                loadingDialog?.dismiss()
                if (response.isSuccessful) {
                    response.body()?.let { surveyResponse ->
                        Log.d("SurveyActivity", "Received response from server: $surveyResponse")
                        startResultActivity(surveyResponse)
                    } ?: showErrorDialog("서버에서 유효한 응답을 받지 못했습니다.")
                } else {
                    showErrorDialog("서버 오류: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<SurveyResultsResponse>, t: Throwable) {
                loadingDialog?.dismiss()
                showErrorDialog("서버에 문제가 생겼습니다.")
                Log.e("SurveyActivity", "Failed to submit responses: ${t.message}")
            }
        })
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this).apply {
            setMessage(message)
            setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            create().show()
        }
    }


    private fun handleErrorResponse(response: Response<SurveyResultsResponse>) {
        Log.e("SurveyActivity", "Response 실패: ${response.code()} - ${response.message()}")

        when (response.code()) {
            400 -> Log.e("SurveyActivity", "Bad Request - 서버가 요청을 이해하지 못함")
            401 -> Log.e("SurveyActivity", "Unauthorized - 인증이 필요함")
            403 -> Log.e("SurveyActivity", "Forbidden - 서버가 요청을 거부함")
            404 -> Log.e("SurveyActivity", "Not Found - 요청한 리소스를 찾을 수 없음")
            500 -> Log.e("SurveyActivity", "Internal Server Error - 서버 내부 오류")
            else -> Log.e("SurveyActivity", "Unknown Error - 알 수 없는 오류: ${response.code()}")
        }

        response.errorBody()?.let {
            val errorBody = it.string()
            Log.e("SurveyActivity", "Error Body: $errorBody")
            runOnUiThread {
                Toast.makeText(this, "오류 발생: $errorBody", Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onPause() {
        super.onPause()
        loadingDialog?.dismiss() // onPause 시 Dialog 닫기
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog?.dismiss() // onDestroy 시 Dialog 닫기
    }

    interface ApiService {
        @POST("response/{seniorId}")
        fun submitSurveyResults(
            @Path("seniorId") seniorId: Int,
            @Body requestBody: RequestBody
        ): Call<SurveyResultsResponse>
    }
    companion object {
        private fun String.toRequestBody() =
            okhttp3.RequestBody.create(MediaType.parse("application/json"), this)
    }
    data class ResponseDTO(
        val questionNumber: String,
        val response: String
    )
    data class SurveyResultsResponse(
        val resultId: Long,
        val date: String,
        val rank: Int,
        val summary: String
    ) : Serializable

    data class SurveyResultsRequest(
        val responseDTOS: List<ResponseDTO>
    )


}
