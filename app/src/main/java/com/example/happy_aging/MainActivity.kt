package com.example.happy_aging

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar()
//
//        configureButton(R.id.startButton, R.drawable.icon_survey, "Start Survey", R.layout.custom_twolines_button_layout)
//        configureButton(R.id.start_ar_button, R.drawable.icon_ar, "Start AR", R.layout.custom_twolines_button_layout)
//        configureButton(R.id.goStore_button, R.drawable.icon_store, "Go to Store", R.layout.button_custom_layout)
//        configureButton(R.id.video_button, R.drawable.icon_video, "Play Video", R.layout.button_custom_layout)

        setupButtonListeners()
    }

    private fun configureButton(buttonId: Int, iconResId: Int, text: String, layoutId: Int) {
        val frameLayout: FrameLayout = findViewById(buttonId)
        val customLayout: View = layoutInflater.inflate(layoutId, null)

        customLayout.findViewById<ImageView>(R.id.button_icon).setImageResource(iconResId)
        customLayout.findViewById<TextView>(R.id.button_text).text = text

        frameLayout.addView(customLayout)
    }


    private fun setupButtonListeners() {
        findViewById<FrameLayout>(R.id.startButtonFrame).setOnClickListener {
            startActivity(Intent(this, StartSurveyActivity::class.java))
        }

        findViewById<FrameLayout>(R.id.start_ar_button).setOnClickListener {
            startActivity(Intent(this, ListActivity::class.java))
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
}
