package com.example.watpool.services.interfaces

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot

interface CoordinateService {
    fun fetchCoordinatesByDriverId(driverId: String): Task<QuerySnapshot>
    fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<QuerySnapshot>
    fun addCoordinate(driverId: String, latitude: Double, longitude: Double): Task<DocumentReference>
}