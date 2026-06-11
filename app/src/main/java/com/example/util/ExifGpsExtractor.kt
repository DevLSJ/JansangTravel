package com.example.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExifGpsExtractor {
    suspend fun extractGpsFromImageUri(context: Context, imageUri: Uri): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val latLong = exifInterface.latLong
                if (latLong != null && latLong.size >= 2) {
                    val lat = latLong[0]
                    val lng = latLong[1]
                    if (lat != 0.0 || lng != 0.0) {
                        return@withContext Pair(lat, lng)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
