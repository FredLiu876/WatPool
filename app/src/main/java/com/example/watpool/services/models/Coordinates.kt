package com.example.watpool.services.models

import com.google.firebase.firestore.DocumentSnapshot

data class Coordinate(
    /*val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val driverId: String = ""*/

    val driverId: String = "",
    val geohash: String = "",
    val id: String = "",
    val latitude: Double = 0.0,
    val location: String = "",
    val longitude: Double = 0.0
)

fun DocumentSnapshot.toCoordinate(): Coordinate {
    return Coordinate(
        driverId = getString("driver_id") ?: "",
        geohash = getString("geohash") ?: "",
        id = getString("id") ?: "",
        latitude = getDouble("latitude") ?: 0.0,
        location = getString("location") ?: "",
        longitude = getDouble("longitude") ?: 0.0
    )
}