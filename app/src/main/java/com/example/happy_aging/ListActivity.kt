package com.example.happy_aging

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog


class ListActivity : AppCompatActivity(), ItemAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var items: List<Item> // Declare the items list here for wider scope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        setupToolbar()


        items = listOf(
            Item("실버카 성인용 보행기", "item1", R.drawable.img_car_walker),
            Item("전동식 의료용 침대", "item2",R.drawable.img_bed),
            Item("2단 바퀴 실내 성인용 보행기", "item3",R.drawable.img_car_walker_two),
            Item("복지용구 목욕의자", "item4",R.drawable.img_chair)
        )


        recyclerView = findViewById(R.id.recycler_view)
        itemAdapter = ItemAdapter(items, this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = itemAdapter
    }
//    private fun showInstructionDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("유의사항")
//            .setMessage("- 평평한 곳에 카메라를 2~5초 비추세요.\n\n- 화면에 흰 점들이 나타나면 화면을 터치하세요.")
//            .setPositiveButton("확인 완료") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .show()
//    }
    private fun setupToolbar() {
        val customToolbar: View = layoutInflater.inflate(R.layout.toolbar_title, null)
        customToolbar.findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        val toolbarTitle: TextView = customToolbar.findViewById(R.id.toolbar_title)
        toolbarTitle.apply {
            text = "낙하 사고 예방템"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(typeface, Typeface.BOLD)
        }

        findViewById<Toolbar>(R.id.toolbar).apply {
            addView(customToolbar)
            setSupportActionBar(this)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    override fun onItemClick(position: Int) {
        val intent = Intent(this, ArActivity::class.java)
        intent.putExtra("SELECTED_ITEM", items[position].id)
        startActivity(intent)
    }
}
