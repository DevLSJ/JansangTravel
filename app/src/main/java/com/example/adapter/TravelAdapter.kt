package com.example.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.db.RecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TravelAdapter(
    private val context: Context,
    private var records: List<RecordEntity>,
    private val onItemClick: (RecordEntity) -> Unit,
    private val onEditClick: (RecordEntity) -> Unit,
    private val onDeleteClick: (RecordEntity) -> Unit
) : RecyclerView.Adapter<TravelAdapter.ViewHolder>() {

    fun updateList(newList: List<RecordEntity>) {
        records = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = records[position]
        holder.bind(item)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelImageLoad()
    }

    override fun getItemCount(): Int = records.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val progressBarImage: ProgressBar = itemView.findViewById(R.id.progressBarImage)
        private val tvPlace: TextView = itemView.findViewById(R.id.tvPlace)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvMemo: TextView = itemView.findViewById(R.id.tvMemo)
        private val tvGpsCoords: TextView = itemView.findViewById(R.id.tvGpsCoords)
        private val layoutGpsTag: View = itemView.findViewById(R.id.layoutGpsTag)
        private var imageLoadJob: kotlinx.coroutines.Job? = null

        fun cancelImageLoad() {
            imageLoadJob?.cancel()
        }

        fun bind(item: RecordEntity) {
            tvPlace.text = item.title
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            tvDate.text = sdf.format(Date(item.createdAt))
            
            tvMemo.text = item.memo

            // Display GPS tags if lat/long are non-zero/valid
            val lat = item.latitude ?: 0.0
            val lng = item.longitude ?: 0.0
            if (lat != 0.0 || lng != 0.0) {
                layoutGpsTag.visibility = View.VISIBLE
                tvGpsCoords.text = String.format("%.4f, %.4f", lat, lng)
            } else {
                layoutGpsTag.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Context Menu / PopupMenu on Long Click
            itemView.setOnLongClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add(0, 1, 0, "기록 수정")
                popup.menu.add(0, 2, 0, "기록 삭제")
                
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onEditClick(item)
                            true
                        }
                        2 -> {
                            onDeleteClick(item)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
                true
            }

            // Async Image loading with ProgressBar using custom Kotlin Coroutine
            imageLoadJob = com.example.util.RecordImageLoader.load(
                context = context,
                imageView = ivPhoto,
                progressBar = progressBarImage,
                record = item,
                scope = CoroutineScope(Dispatchers.Main)
            )
        }
    }
}
