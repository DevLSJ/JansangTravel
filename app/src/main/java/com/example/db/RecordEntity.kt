package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val memo: String,
    val imageUri: String,
    val createdAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val imageType: String = "URI",
    val imageRef: String? = null
) : Serializable
