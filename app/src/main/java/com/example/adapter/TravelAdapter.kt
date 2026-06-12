package com.example.adapter

import android.content.Context
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
import com.example.util.RecordImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

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
        holder.bind(records[position])
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
        private val tvLocation: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvMemo)
        private val tvGpsCoords: TextView = itemView.findViewById(R.id.tvGpsCoords)
        private val layoutGpsTag: View = itemView.findViewById(R.id.layoutGpsTag)
        private var imageLoadJob: Job? = null

        fun cancelImageLoad() {
            imageLoadJob?.cancel()
            imageLoadJob = null
        }

        fun bind(item: RecordEntity) {
            cancelImageLoad()

            val (location, description) = splitMemo(item.memo)
            tvPlace.text = item.title
            tvLocation.text = location
            tvDescription.text = description

            val lat = item.latitude ?: 0.0
            val lng = item.longitude ?: 0.0
            if (lat != 0.0 || lng != 0.0) {
                layoutGpsTag.visibility = View.VISIBLE
                tvGpsCoords.text = String.format("%.4f, %.4f", lat, lng)
            } else {
                layoutGpsTag.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }

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

            imageLoadJob = RecordImageLoader.load(
                context = context,
                imageView = ivPhoto,
                progressBar = progressBarImage,
                record = item,
                scope = CoroutineScope(Dispatchers.Main)
            )
        }

        private fun splitMemo(memo: String): Pair<String, String> {
            val parts = memo.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return when {
                parts.size >= 2 -> parts.first() to parts.drop(1).joinToString(" ")
                parts.size == 1 -> "" to parts.first()
                else -> "" to ""
            }
        }
    }
}
