package com.example.db

import java.io.Serializable

data class RecordEntity(
    val id: Long = 0,
    val title: String,
    val memo: String,
    val imageUri: String,
    val createdAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val imageType: String = "URI",
    val imageRef: String? = null
) : Serializable
