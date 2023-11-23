package com.example.happy_aging

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListActivity : AppCompatActivity(), ItemAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var items: List<Item> // Declare the items list here for wider scope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        items = listOf(
            Item("지팡이", "item1", R.drawable.img_item1),
            Item("매트", "item2",R.drawable.img_item2),
            Item("손잡이", "item3",R.drawable.img_item3)
        )

        recyclerView = findViewById(R.id.recycler_view)
        itemAdapter = ItemAdapter(items, this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = itemAdapter
    }

    override fun onItemClick(position: Int) {
        val intent = Intent(this, ArActivity::class.java)
        intent.putExtra("SELECTED_ITEM", items[position].id)
        startActivity(intent)
    }
}
