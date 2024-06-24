package com.example.watpool.services.firebase_services

import com.example.watpool.services.interfaces.CoordinateService
import com.example.watpool.services.models.Coordinate
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore

import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class FirebaseCoordinateService: CoordinateService {
    private val database = Firebase.firestore
    private val coordinatesRef: CollectionReference = database.collection("coordinates")

    // Distance between 2 coords using the haversine
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Earth's radius in kilometers
        val radiansLat1 = Math.toRadians(lat1)
        val radiansLong1 = Math.toRadians(lon1)
        val radiansLat2 = Math.toRadians(lat2)
        val radiansLong2 = Math.toRadians(lon2)

        val dlat = radiansLat2 - radiansLat1
        val dlong = radiansLong2 - radiansLong1

        val component = sin(dlat / 2).pow(2) + cos(radiansLat1) * cos(radiansLat2) * sin(dlong / 2).pow(2)
        val angularDistance = 2 * atan2(sqrt(component), sqrt(1 - component))

        return R * angularDistance
    }

    override fun fetchCoordinatesByDriverId(driverId: String): Task<QuerySnapshot> {
        return coordinatesRef.whereEqualTo("driver_id", driverId).get()
    }

    override fun addCoordinate(driverId: String, latitude: Double, longitude: Double): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val coordinate = hashMapOf(
            "id" to id,
            "latitude" to latitude,
            "longitude" to longitude,
            "driver_id" to driverId
        )

        return coordinatesRef.add(coordinate)
    }

    override fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<QuerySnapshot> {
        // Find the bounding box for quick filtering
        val latDelta = radiusInKm / 111.32
        val lonDelta = radiusInKm / (111.32 * cos(Math.toRadians(latitude)))

        val minLat = latitude - latDelta
        val maxLat = latitude + latDelta
        val minLon = longitude - lonDelta
        val maxLon = longitude + lonDelta

        return coordinatesRef.where(
            Filter.and(
                Filter.greaterThanOrEqualTo("latitude", minLat),
                Filter.lessThanOrEqualTo("latitude", maxLat),
                Filter.greaterThanOrEqualTo("longitude", minLon),
                Filter.lessThanOrEqualTo("longitude", maxLon)
            )
        ).get().addOnSuccessListener { querySnapshot ->
            val filteredDocs = querySnapshot.documents.filter { doc ->
                val driverLatitude = doc.getDouble("latitude") ?: return@filter false
                val driverLongitude = doc.getDouble("longitude") ?: return@filter false
                val distance = calculateDistance(latitude, longitude, driverLatitude, driverLongitude)
                distance <= radiusInKm
            }

            // Update the QuerySnapshot with filtered results
            querySnapshot.documents.clear()
            querySnapshot.documents.addAll(filteredDocs)
        }
    }
}