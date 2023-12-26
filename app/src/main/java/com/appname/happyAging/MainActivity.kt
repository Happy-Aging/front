package com.appname.happyAging

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar()
        setupButtonListeners()
    }

    private fun setupToolbar() {
        val customToolbar: View = layoutInflater.inflate(R.layout.toolbar_title, null)
        customToolbar.findViewById<ImageView>(R.id.back_button).setImageResource(R.drawable.icon)
        customToolbar.findViewById<TextView>(R.id.toolbar_title).text = "해피에이징"

        findViewById<Toolbar>(R.id.main_toolbar).apply {
            addView(customToolbar)
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupButtonListeners() {
        findViewById<FrameLayout>(R.id.startButtonFrame).setOnClickListener {
            startActivity(Intent(this, StartSurveyActivity::class.java))
        }

        findViewById<FrameLayout>(R.id.start_ar_button).setOnClickListener {
            checkArCoreSupport()
        }

        findViewById<FrameLayout>(R.id.goStore_button).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smartstore.naver.com/happy5678"))
            startActivity(browserIntent)
        }

        findViewById<FrameLayout>(R.id.video_button).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@user-pu6cq3vw3v"))
            startActivity(browserIntent)
        }
    }

    private fun checkArCoreSupport() {
        when (val availability = ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                startActivity(Intent(this, ListActivity::class.java))
            }
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                Toast.makeText(this, "ARCore 업데이트 또는 설치가 필요합니다.", Toast.LENGTH_LONG).show()
                showArCoreUpdateDialog()
            }
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                showArCoreUnsupportedDialog()
            }
            else -> {
                Toast.makeText(this, "ARCore를 사용할 수 없습니다: $availability", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun showArCoreUpdateDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("ARCore 업데이트 필요")
            .setMessage("해당 서비스를 이용하기 위해서는 ARCore 업데이트 또는 설치가 필요합니다.")
            .setPositiveButton("업데이트 또는 설치하러 가기") { dialogInterface, _ ->
                openArCoreInPlayStore()
                dialogInterface.dismiss()
            }
            .setNegativeButton("종료", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
    }


    private fun showArCoreUnsupportedDialog() {
        val message = """
            ARCore를 지원하지 않는 이유:
            - 하드웨어 사양 미달
            - 운영 체제 버전 (Android 7.0 이상 필요)
            - 센서 부재 (자이로스코프와 가속도계 필요)
            - 카메라 성능
            - 제조업체의 지원 부족
            - Google Play 서비스의 부재
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("ARCore 지원 불가")
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun openArCoreInPlayStore() {
        val appPackageName = "com.google.ar.core" // ARCore의 패키지 이름
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (anfe: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }

}
