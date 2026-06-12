package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.R
import com.example.db.RecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object RecordImageLoader {

    fun load(
        context: Context,
        imageView: ImageView,
        progressBar: ProgressBar?,
        record: RecordEntity,
        scope: CoroutineScope
    ): Job? {
        progressBar?.visibility = View.VISIBLE
        imageView.setImageDrawable(null)

        if (record.imageType == "DRAWABLE") {
            imageView.setImageResource(resolveDrawableResId(context, record) ?: android.R.drawable.ic_menu_gallery)
            progressBar?.visibility = View.GONE
            return null
        }

        val uriString = record.imageUri
        val hasValidUri = uriString.startsWith("content://") || uriString.startsWith("file://")

        if (!hasValidUri) {
            imageView.setImageResource(resolveDrawableResId(context, record) ?: android.R.drawable.ic_menu_gallery)
            progressBar?.visibility = View.GONE
            return null
        }

        return scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(uriString)).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                            inSampleSize = 2
                        })
                    }
                } catch (e: Exception) {
                    null
                }
            }

            progressBar?.visibility = View.GONE
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(resolveDrawableResId(context, record) ?: android.R.drawable.ic_menu_gallery)
            }
        }
    }

    private fun resolveDrawableResId(context: Context, record: RecordEntity): Int? {
        val explicitName = record.imageRef
            ?.substringBeforeLast('.')
            ?.trim()
            ?.lowercase()

        val explicitResId = explicitName
            ?.takeIf { it.matches(Regex("[a-z0-9_]+")) }
            ?.let { context.resources.getIdentifier(it, "drawable", context.packageName) }
            ?.takeIf { it != 0 }

        if (explicitResId != null) return explicitResId

        return when (record.title) {
            "해운대" -> R.drawable.img_haeundae
            "한라산" -> R.drawable.img_hallasan
            "경복궁" -> R.drawable.img_gyeongbokgung
            else -> null
        }
    }
}
