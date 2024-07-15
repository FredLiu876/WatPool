package com.example.watpool.services

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await
import com.example.watpool.services.FirebaseService

data class TripConfirmation(
    val id: String = "",
    val to: String = "",
    val from: String = "",
    val trip_date: String = "",
    val driver_id: String = "",
    val driver: String = ""
)

class TripListService {
    suspend fun fetchAllConfirmedTripsByRiderId(riderId: String): List<TripConfirmation> {
        val db = FirebaseService()
        val tripConfirmations = db.fetchTripConfirmationByRiderId(riderId).await()
        val tripIds = tripConfirmations.documents.mapNotNull { it.getString("tripId") }
        val tripDetails = db.fetchTripsByTripsId(tripIds).await()
        val fetchedTrips = tripDetails.mapNotNull { document ->
            document.toObject(TripConfirmation::class.java)
        }
        val fullTripDetails = fetchedTrips.map { trip ->
            val driverName = db.fetchUsersById(trip.driver_id).await().documents.mapNotNull { it.getString("driver_id") } [0]
            trip.copy(driver = driverName ?: "Unknown")
        }
        return fullTripDetails
    }
}