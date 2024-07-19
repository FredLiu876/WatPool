package com.example.watpool.services.models

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Trip(
    val driverId: String = "",
    val endingCoordinate: String = "",
    val from: String = "",
    val id: String = "",
    val isRecurring: Boolean = false,
    val passengers: List<String> = emptyList(),
    val startingCoordinate: String = "",
    val to: String = "",
    val tripDate: Date? = null
)

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
}