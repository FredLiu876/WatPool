package com.example.watpool.services.models

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Trip(
    val id: String,
    val driverId: String,
    val endGeohash: String,
    val endingCoordinate: String,
    val from: String,
    val isRecurring: Boolean,
    val maxPassengers: Int,
    val startGeohash: String,
    val startingCoordinate: String,
    val to: String,
    val tripDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/*
 fun DocumentSnapshot.toTrip(): Trip {
    return Trip(
        driverId = getString("driver_id") ?: "",
        endingCoordinate = getString("ending_coordinate") ?: "",
        from = getString("from") ?: "",
        id = getString("id") ?: "",
        isRecurring = getBoolean("is_recurring") ?: false,
        passengers = get("passengers") as? List<String> ?: emptyList(),
        startingCoordinate = getString("starting_coordinate") ?: "",
        to = getString("to") ?: "",
        tripDate = getTimestamp("trip_date")?.toDate()
    )
}*/
