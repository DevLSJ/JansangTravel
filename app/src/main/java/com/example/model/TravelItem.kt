package com.example.model

import java.io.Serializable

data class TravelItem(
    val no: Long,
    val place: String,
    val visitDate: String,
    val memo: String,
    val photoUri: String?,
    val latitude: Double,
    val longitude: Double
) : Serializable
