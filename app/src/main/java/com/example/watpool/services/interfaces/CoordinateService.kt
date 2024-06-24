package com.example.watpool.services.interfaces

import com.example.watpool.services.models.Coordinate
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

interface CoordinateService {
    fun fetchCoordinatesByDriverId(driverId: String): Task<QuerySnapshot>
    fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<MutableList<DocumentSnapshot>>
    fun addCoordinate(driverId: String, latitude: Double, longitude: Double): Task<DocumentReference>
}