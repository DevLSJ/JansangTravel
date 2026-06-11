package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.R
import com.example.db.RecordEntity
import kotlinx.coroutines.*

object RecordImageLoader {

    fun load(
        context: Context,
        imageView: ImageView,
        progressBar: ProgressBar?,
        record: RecordEntity,
        scope: CoroutineScope
    ): Job? {
        progressBar?.visibility = View.VISIBLE
        imageView.setImageBitmap(null)

        if (record.imageType == "DRAWABLE") {
            val cleanName = (record.imageRef ?: "").substringBeforeLast('.').trim().lowercase()
            var resolvedResId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
            if (resolvedResId == 0) {
                resolvedResId = when {
                    cleanName.contains("hallasan") -> R.drawable.sample_mountain
                    cleanName.contains("gyeongbokgung") -> R.drawable.sample_palace
                    cleanName.contains("haeundae") -> R.drawable.sample_beach
                    else -> 0
                }
            }
            if (resolvedResId != 0) {
                imageView.setImageResource(resolvedResId)
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            progressBar?.visibility = View.GONE
            return null
        }

        val uriString = record.imageUri
        val hasValidUri = !uriString.isNullOrEmpty() && (uriString.startsWith("content://") || uriString.startsWith("file://"))

        if (hasValidUri) {
            return scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(uriString)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 2 // balance memory and quality
                        }
                        val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream?.close()
                        bmp
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                progressBar?.visibility = View.GONE
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    loadResourceOrFallback(context, imageView, progressBar, uriString, record.title)
                }
            }
        } else {
            loadResourceOrFallback(context, imageView, progressBar, uriString, record.title)
            return null
        }
    }

    private fun loadResourceOrFallback(
        context: Context,
        imageView: ImageView,
        progressBar: ProgressBar?,
        uriString: String?,
        placeName: String
    ) {
        var resolvedResId = 0
        if (!uriString.isNullOrEmpty()) {
            val cleanName = uriString.substringBeforeLast('.').trim().lowercase()
            resolvedResId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        }

        if (resolvedResId == 0) {
            val nameLower = placeName.lowercase()
            resolvedResId = when {
                nameLower.contains("한라산") || nameLower.contains("hallasan") -> R.drawable.sample_mountain
                nameLower.contains("해운대") || nameLower.contains("haeundae") -> R.drawable.sample_beach
                nameLower.contains("경복궁") || nameLower.contains("gyeongbokgung") -> R.drawable.sample_palace
                else -> 0
            }
        }

        if (resolvedResId != 0) {
            imageView.setImageResource(resolvedResId)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        progressBar?.visibility = View.GONE
    }
}
