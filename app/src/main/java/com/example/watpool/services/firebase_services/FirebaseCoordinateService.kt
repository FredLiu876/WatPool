package com.example.watpool.services.firebase_services

import android.util.Log
import com.example.watpool.services.interfaces.CoordinateService
import com.example.watpool.services.models.Coordinate
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
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

    override fun fetchCoordinatesById(id: String): Task<QuerySnapshot> {
        return coordinatesRef.whereEqualTo("id", id).get()
    }

    override fun fetchCoordinatesByDocumentId(id: String): Task<DocumentSnapshot> {
        return coordinatesRef.document(id).get()
    }

    override fun fetchCoordinatesByDriverId(driverId: String): Task<QuerySnapshot> {
        return coordinatesRef.whereEqualTo("driver_id", driverId).get()
    }

    override fun addCoordinate(driverId: String, latitude: Double, longitude: Double, location: String): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))
        val coordinate = hashMapOf(
            "id" to id,
            "latitude" to latitude,
            "longitude" to longitude,
            "driver_id" to driverId,
            "geohash" to hash,
            "location" to location
        )

        return coordinatesRef.add(coordinate)
    }

    override fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<MutableList<DocumentSnapshot>> {
        // Find the bounding box for quick filtering
        val center = GeoLocation(latitude, longitude)
        val radiusInM = radiusInKm * 1000

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = coordinatesRef
                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        return Tasks.whenAllComplete(tasks)
            .continueWith {
                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap!!.documents) {
                        val lat = doc.getDouble("latitude")!!
                        val lng = doc.getDouble("longitude")!!
                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                        if (distanceInM <= radiusInM) {
                            matchingDocs.add(doc)
                        }
                    }
                }

                matchingDocs
            }

    }

}