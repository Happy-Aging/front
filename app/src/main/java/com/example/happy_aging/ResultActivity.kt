package com.example.happy_aging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
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
import java.io.Serializable


class ResultActivity : AppCompatActivity() {

    private lateinit var textViewUserName: TextView
    private lateinit var textSurveyResult: TextView
    private lateinit var textDate: TextView
    private lateinit var textRank: TextView
    private lateinit var buttonDownloadReport: Button
    private lateinit var progressBarDownload: ProgressBar
    private lateinit var imageViewRankIndicator: ImageView
    
    private var savedPdfFilePath: String? = null
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


//        buttonViewPdfWeb.setOnClickListener { openPdfInWebBrowser() }
    }

//    private fun openPdfInWebBrowser() {
//        Log.d("ResultActivity", "openPdfInWebBrowser 실행")
//
//        savedPdfFilePath?.let { filePath ->
//            val fileUri = FileProvider.getUriForFile(
//                this,
//                "${BuildConfig.APPLICATION_ID}.provider",
//                File(filePath)
//            )
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(fileUri, "application/pdf")
//                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//            }
//            startActivity(intent)
//        } ?: Toast.makeText(this, "PDF 파일이 아직 다운로드되지 않았습니다.", Toast.LENGTH_SHORT).show()
//    }

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
    private fun setRankIndicatorPosition(indicator: ImageView, progressBar: ProgressBar, rank: Int) {
        val maxRank = progressBar.max
        progressBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                progressBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = progressBar.width
                val indicatorPosition = (width * rank / maxRank.toFloat()).toInt()
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

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
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
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun showDownloadNotification(progress: Int, isComplete: Boolean) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("보고서 다운로드")
            .setContentText(if (isComplete) "다운로드 완료" else "다운로드 중...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, progress, false)

        if (isComplete) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("file_path_here") // 다운로드한 파일의 경로
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            builder.setContentIntent(pendingIntent)
            builder.setAutoCancel(true) // 사용자가 알림을 탭하면 자동으로 알림이 사라집니다.
        }

        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }


    interface ApiService {
        @GET("/survey/{resultId}/download")
        fun downloadReport(@Path("resultId") resultId: Long): Call<ResponseBody>
    }
    data class SurveyResultsResponse(
        val resultId: Long,
        val date: String,
        val rank: Int,
        val summary: String
    ) : Serializable

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "download_notification_channel"
        private const val CHANNEL_NAME = "Download Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for download progress"
    }




}
