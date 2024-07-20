package com.example.watpool.services.models

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.watpool.services.firebase_services.FirebaseTripsService
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
data class Trips (
    val driverId: String = "",
    val startingCoordinateId: String = "",
    val endingCoordinateId: String = "",
    val startGeohash: String = "",
    val endGeohash: String = "",
    val startLatLng: LatLng = LatLng(0.0,0.0),
    val endLatLng: LatLng = LatLng(0.0,0.0),
    val tripDate: LocalDate = LocalDate.now(),
    val maxPassengers: String = "",
    val isRecurring: Boolean = false,
    val recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY,
    val recurringEndDate: LocalDate = LocalDate.now()

)