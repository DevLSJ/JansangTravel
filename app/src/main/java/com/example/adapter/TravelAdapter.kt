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
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.model.TravelItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelAdapter(
    private val context: Context,
    private var travelList: List<TravelItem>,
    private val onItemClick: (TravelItem) -> Unit,
    private val onEditClick: (TravelItem) -> Unit,
    private val onDeleteClick: (TravelItem) -> Unit
) : RecyclerView.Adapter<TravelAdapter.ViewHolder>() {

    fun updateList(newList: List<TravelItem>) {
        travelList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = travelList[position]
        holder.bind(item)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelImageLoad()
    }

    override fun getItemCount(): Int = travelList.size

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

        fun bind(item: TravelItem) {
            tvPlace.text = item.place
            tvDate.text = item.visitDate
            tvMemo.text = item.memo

            // Display GPS tags if lat/long are non-zero/valid
            if (item.latitude != 0.0 || item.longitude != 0.0) {
                layoutGpsTag.visibility = View.VISIBLE
                tvGpsCoords.text = String.format("%.4f, %.4f", item.latitude, item.longitude)
            } else {
                layoutGpsTag.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Context Menu Listener (Long Click)
            itemView.setOnCreateContextMenuListener { menu, _, _ ->
                val menuEdit = menu.add(0, 1, 0, "기록 수정")
                val menuDelete = menu.add(0, 2, 0, "기록 삭제")

                menuEdit.setOnMenuItemClickListener {
                    onEditClick(item)
                    true
                }
                menuDelete.setOnMenuItemClickListener {
                    onDeleteClick(item)
                    true
                }
            }

            // Async Image loading with ProgressBar using custom Kotlin Coroutine
            loadPhotoAsync(item.photoUri)
        }

        private fun loadPhotoAsync(uriString: String?) {
            cancelImageLoad()
            progressBarImage.visibility = View.VISIBLE
            ivPhoto.setImageBitmap(null)

            if (uriString.isNullOrEmpty()) {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                progressBarImage.visibility = View.GONE
                return
            }

            // Execute asynchronous processing using main block
            imageLoadJob = CoroutineScope(Dispatchers.Main).launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(uriString)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // Decode at 1/4 the resolution to protect JVM heap memory
                        }
                        val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream?.close()
                        bmp
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                progressBarImage.visibility = View.GONE
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap)
                } else {
                    ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }
    }
}
