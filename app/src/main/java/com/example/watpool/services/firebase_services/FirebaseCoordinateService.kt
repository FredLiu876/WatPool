package com.example.watpool.services.firebase_services

import com.example.watpool.services.interfaces.CoordinateService
import com.example.watpool.services.models.Coordinate
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class FirebaseCoordinateService: CoordinateService {
    private val database = Firebase.database
    private val coordinatesRef: DatabaseReference = database.getReference("coordinates")

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

    override fun fetchCoordinatesByDriverId(driverId: String): Task<DataSnapshot> {
        return coordinatesRef.orderByChild("driverId").equalTo(driverId).get()
    }

    override fun addCoordinate(driverId: String, latitude: Double, longitude: Double): Task<Void> {
        val id: String = UUID.randomUUID().toString()
        val coordinate = Coordinate(id, latitude, longitude, driverId)
        return coordinatesRef.child(id).setValue(coordinate)
    }

    override fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<DataSnapshot> {
        return coordinatesRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.filter { coordSnapshot ->
                val coord = coordSnapshot.getValue(Coordinate::class.java)
                coord?.let {
                    val distance = calculateDistance(latitude, longitude, it.latitude, it.longitude)
                    distance <= radiusInKm
                } ?: false
            }
        }
    }
}