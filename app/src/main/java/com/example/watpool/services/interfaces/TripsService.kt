package com.example.watpool.services.interfaces
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.watpool.services.firebase_services.FirebaseTripsService
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import java.time.LocalDate
import java.time.LocalTime

interface TripsService {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createTrip(driverId: String, startingCoordinateId: String, endingCoordinateId: String, startGeohash: String, endGeohash: String, tripDate: LocalDate, maxPassengers: String, isRecurring: Boolean = false, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY, recurringEndDate: LocalDate = LocalDate.now(), tripTime: LocalTime = LocalTime.now()): Task<DocumentReference>
  
    @RequiresApi(Build.VERSION_CODES.O)
    fun createTripPosting(userId: String, startingCoordinateId: String, endingCoordinateId: String, startGeohash: String, endGeohash: String, tripDate: LocalDate, isRecurring: Boolean = false, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY, recurringEndDate: LocalDate = LocalDate.now(), tripTime: LocalTime = LocalTime.now()): Task<DocumentReference>
    
    fun createTripConfirmation(tripId: String, confirmationDate: LocalDate, riderId: String): Task<DocumentReference>

    fun fetchTripsByTripIds(tripIds: List<String>): Task<QuerySnapshot>

    fun fetchTripsByDriverId(driverId: String): Task<QuerySnapshot>

    fun fetchTripsByRiderId(riderId: String): Task<QuerySnapshot>

    fun fetchTripConfirmationByRiderId(riderId: String): Task<QuerySnapshot>

    fun fetchTripConfirmationByTripIdAndRiderId(tripId: String, riderId: String): Task<QuerySnapshot>

    fun deleteTripConfirmation(documentId: String)

    fun acceptTripPosting(driverId: String, tripId: String): Task<Void>

    fun updateTripCoordinates(tripId: String, startingCoordinateId: String, endingCoordinateId: String): Task<Void>
    // update num passengers
    fun updatePassengerCount(tripId: String, newCount: Int): Task<Void>

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTripDate(tripId: String, date: LocalDate): Task<Void>

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTripTime(tripId: String, time: LocalTime): Task<Void>

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeTripRecurring(tripId: String, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek, recurringEndDate: LocalDate): Task<Void>

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeTripNonRecurring(tripId: String): Task<Void>

    fun fetchTripsByLocation(latitude: Double, longitude: Double, radiusInKm: Double, startFilter: Boolean): Task<MutableList<DocumentSnapshot>>

    fun addPassenger(tripId: String, newPassengerId: String): Task<Void>

    fun removePassenger(tripId: String, passengerId: String): Task<Void>

}