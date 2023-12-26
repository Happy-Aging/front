package com.appname.happyAging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(private val itemList: List<Item>, private val listener: OnItemClickListener) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.item_image)
        val nameView: TextView = view.findViewById(R.id.item_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        holder.imageView.setImageResource(item.imageResId)
        holder.nameView.text = item.name
        holder.itemView.setOnClickListener { listener.onItemClick(position) }
    }

    override fun getItemCount() = itemList.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
