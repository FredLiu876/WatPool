package com.example.watpool.services.interfaces

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot

interface CoordinateService {
    fun fetchCoordinatesByDriverId(driverId: String): Task<DataSnapshot>
    fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<DataSnapshot>
    fun addCoordinate(driverId: String, latitude: Double, longitude: Double): Task<Void>
}