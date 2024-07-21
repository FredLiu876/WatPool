package com.example.watpool.services.models

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.watpool.services.firebase_services.FirebaseTripsService
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.PropertyName
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
    val recurringEndDate: LocalDate = LocalDate.now(),
    val timeString: String = ""

)

data class MyLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val stability: Double = 0.0
) {
    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    companion object {
        fun fromLatLng(latLng: LatLng): MyLatLng {
            return MyLatLng(latLng.latitude, latLng.longitude)
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
data class Postings (
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("to")
    @set:PropertyName("to")
    var to: String = "",
    @get:PropertyName("from")
    @set:PropertyName("from")
    var from: String = "",
    @get:PropertyName("driver_id")
    @set:PropertyName("driver_id")
    var driverId: String = "",
    @get:PropertyName("starting_coordinate")
    @set:PropertyName("starting_coordinate")
    var startingCoordinateId: String = "",
    @get:PropertyName("ending_coordinate")
    @set:PropertyName("ending_coordinate")
    var endingCoordinateId: String = "",
    @get:PropertyName("start_geohash")
    @set:PropertyName("start_geohash")
    var startGeohash: String = "",
    @get:PropertyName("end_geohash")
    @set:PropertyName("end_geohash")
    var endGeohash: String = "",
    @get:PropertyName("trip_date")
    @set:PropertyName("trip_date")
    var tripDate: String = "",
    @get:PropertyName("max_passengers")
    @set:PropertyName("max_passengers")
    var maxPassengers: String = "",
    @get:PropertyName("is_recurring")
    @set:PropertyName("is_recurring")
    var isRecurring: Boolean = false,
    @get:PropertyName("endLatLng")
    @set:PropertyName("endLatLng")
    var endLatLng: MyLatLng = MyLatLng(),
    @get:PropertyName("startLatLng")
    @set:PropertyName("startLatLng")
    var startLatLng: MyLatLng = MyLatLng(),
    var startCoords: Coordinate = Coordinate("", 0.0, 0.0, "","",""),
    var endCoords: Coordinate = Coordinate("", 0.0, 0.0, "","",""),
)


