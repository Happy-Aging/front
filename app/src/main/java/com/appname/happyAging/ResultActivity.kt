package com.appname.happyAging

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class ResultActivity : AppCompatActivity() {

    private lateinit var textViewUserName: TextView
    private lateinit var textSurveyResult: TextView
    private lateinit var textDate: TextView
    private lateinit var textRank: TextView
    private lateinit var buttonDownloadReport: Button
    private lateinit var buttonGoReport: Button
    private lateinit var progressBarDownload: ProgressBar
    private lateinit var imageViewRankIndicator: ImageView
    
    private var userNameFixed: String? = null

    private val baseUrl = "http://13.125.35.235:8080/"

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        initializeViews()
        loadDate()
        setupToolbar()
    }

    private fun initializeViews() {
        textViewUserName = findViewById(R.id.textViewUserName)
        textDate = findViewById(R.id.textDate)
        textRank = findViewById(R.id.textRank)
        textSurveyResult = findViewById(R.id.textSurveyResult)
        buttonDownloadReport = findViewById(R.id.buttonDownloadReport)
        buttonGoReport = findViewById(R.id.buttonGoReport)
        imageViewRankIndicator = findViewById(R.id.imageViewRankIndicator)
        progressBarDownload = findViewById<ProgressBar>(R.id.progressBarDownload)
    }

    private fun loadDate() {
        Log.d("ResultActivity", "loadDate 메서드 실행됨")

        val resultId = intent.getStringExtra("resultId") ?: "-1"
        val date = intent.getStringExtra("date") ?: "Unknown Date"
        val rank = intent.getStringExtra("rank") ?: "-1"
        val summary = intent.getStringExtra("summary") ?: "No summary available"
        userNameFixed = intent.getStringExtra("userName")

        textViewUserName.text = "$userNameFixed"
        textDate.text = date
        textRank.text = rank
        textSurveyResult.text = if (summary.length > 500) summary.substring(0, 500) + "..." else summary

        setRankIndicatorPosition(imageViewRankIndicator, progressBarDownload, rank.toInt())

        buttonDownloadReport.setOnClickListener {
            if (resultId != "-1") downloadReport(resultId.toLong())
            else Toast.makeText(this, "결과 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        val customToolbar: View = layoutInflater.inflate(R.layout.toolbar_title, null)
        customToolbar.findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        val toolbarTitle: TextView = customToolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.apply {
            text = "낙상 위험도 측정 결과"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(typeface, Typeface.BOLD)
        }

        findViewById<Toolbar>(R.id.result_toolbar).apply {
            addView(customToolbar)
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }
//    private fun setRankIndicatorPosition(indicator: ImageView, progressBar: ProgressBar, rank: Int) {
//        val maxRank = progressBar.max
//        progressBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                progressBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                val width = progressBar.width
//                val indicatorPosition = (width * rank / maxRank.toFloat()).toInt()
//                val layoutParams = indicator.layoutParams as RelativeLayout.LayoutParams
//                layoutParams.marginStart = indicatorPosition - (indicator.width / 2)
//                indicator.layoutParams = layoutParams
//            }
//        })
//    }

    private fun setRankIndicatorPosition(indicator: ImageView, progressBar: ProgressBar, rank: Int) {
        val maxRank = progressBar.max
        progressBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                progressBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = progressBar.width
                // Adjusting the calculation for the indicator position
                val indicatorPosition = ((width * (rank - 0.9)) / (maxRank - 0.9).toFloat()).toInt()
                val layoutParams = indicator.layoutParams as RelativeLayout.LayoutParams
                layoutParams.marginStart = indicatorPosition - (indicator.width / 2)
                indicator.layoutParams = layoutParams
            }
        })
    }


    private fun downloadReport(resultId: Long) {
        apiService.downloadReport(resultId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    savePdfToFileSystem(response.body()!!)
                } else {
                    Toast.makeText(this@ResultActivity, "파일 다운로드 실패: 서버 응답 없음", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ResultActivity, "파일 다운로드 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
    private fun updateDownloadPathLayout(filePath: String) {
        val layoutDownPath = findViewById<LinearLayout>(R.id.layoutDownPath)
        val textDownPath = findViewById<TextView>(R.id.textDownPath)

        layoutDownPath.visibility = View.VISIBLE
        textDownPath.text = filePath
    }

    private fun savePdfToFileSystem(body: ResponseBody) {
        try {
            val fileName = "${userNameFixed}_낙상위험도조사결과.pdf"

            // '내 문서' 폴더에 파일을 저장합니다.
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(documentsDir, fileName)
            updateDownloadPathLayout(documentsDir.toString())
            Log.e("ResultActivity", "파일 다운 경로 -> $documentsDir")


            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)

                buttonGoReport.setOnClickListener {
                    openPdfFile(file)
                }

                showDownloadCompleteNotification(this@ResultActivity, file) // 수정된 부분
                Toast.makeText(this, "보고서가 '내 문서' 폴더에 저장되었습니다", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Log.e("ResultActivity", "파일 저장 실패", e)
                Toast.makeText(this, "파일 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "파일 저장 중 오류 발생", e)
            Toast.makeText(this, "파일 저장 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openPdfFile(file: File) {
        val fileUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    fun showDownloadCompleteNotification(context: Context, file: File) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0 // 이전 버전에서는 플래그가 필요하지 않습니다.
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, "download_channel_id")
            .setContentTitle("다운로드 완료")
            .setContentText("PDF 파일이 다운로드 되었습니다. 탭하여 열기")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }


    interface ApiService {
        @GET("/survey/{resultId}/download")
        fun downloadReport(@Path("resultId") resultId: Long): Call<ResponseBody>
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "download_notification_channel"
        private const val CHANNEL_NAME = "Download Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for download progress"
    }




}
