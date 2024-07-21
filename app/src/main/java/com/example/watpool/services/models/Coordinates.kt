package com.example.watpool.services.models

import com.firebase.geofire.core.GeoHash

data class Coordinate(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val driverId: String = "",
    val location: String ="",
    val geoHash: String = ""
)