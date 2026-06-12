package com.example.util

import android.content.Context
import android.content.pm.PackageManager

object MapsApiKeyValidator {
    private const val META_DATA_KEY = "com.google.android.geo.API_KEY"

    fun getManifestApiKey(context: Context): String? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString(META_DATA_KEY)
        } catch (e: Exception) {
            null
        }
    }

    fun isConfigured(apiKey: String?): Boolean {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) return false

        val upper = key.uppercase()
        return upper != "YOUR_GOOGLE_MAPS_API_KEY" &&
            upper != "REPLACE_WITH_YOUR_GOOGLE_MAPS_API_KEY" &&
            upper != "PLACEHOLDER_KEY" &&
            !upper.contains("PLACEHOLDER")
    }

    fun message(apiKey: String?): String {
        val keyState = if (apiKey.isNullOrBlank()) {
            "현재 앱에 Google Maps API 키가 설정되어 있지 않습니다."
        } else {
            "현재 앱에 placeholder Google Maps API 키가 들어가 있습니다."
        }

        return "$keyState\n.env 또는 local.properties에 MAPS_API_KEY를 설정한 뒤 다시 빌드해주세요."
    }
}
