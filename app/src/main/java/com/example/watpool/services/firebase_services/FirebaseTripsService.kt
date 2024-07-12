package com.example.watpool.services.firebase_services


import android.os.Build
import androidx.annotation.RequiresApi
import com.example.watpool.services.interfaces.TripsService
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.util.UUID

class FirebaseTripsService: TripsService {
    private val database = Firebase.firestore
    private val tripsRef: CollectionReference = database.collection("trips")
    private val tripsConfirmationRef: CollectionReference = database.collection("trips_confirmation")

    enum class DayOfTheWeek(val day: String) {
        SUNDAY ("SUNDAY"), MONDAY ("MONDAY"), TUESDAY ("TUESDAY"),
        WEDNESDAY ("WEDNESDAY"), THURSDAY ("THURSDAY"), FRIDAY ("FRIDAY"), SATURDAY ("SATURDAY")
    }

    // fetches the last trip and creates a clone
//    override fun cloneTrip(tripId: String): Task<DocumentReference> {
//        val oldTrip = tripsRef.whereEqualTo("id", tripId).get()
//        return oldTrip
//    }

    // driver posting a new trip
    @RequiresApi(Build.VERSION_CODES.O)
    override fun createTrip(driverId: String, startingCoordinateId: String, endingCoordinateId: String, tripDate: LocalDate, maxPassengers: String, isRecurring: Boolean, recurringDayOfTheWeek: DayOfTheWeek, recurringEndDate: LocalDate): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val trip = hashMapOf(
            "id" to id,
            "starting_coordinate" to startingCoordinateId,
            "ending_coordinate" to endingCoordinateId,
            "max_passengers" to maxPassengers,
            "is_recurring" to isRecurring,
            "trip_date" to tripDate.toString(),
            "driver_id" to driverId
        )

        if (isRecurring) {
            trip["recurring_day"] = recurringDayOfTheWeek.day
            trip["recurring_end_dat"] = recurringEndDate.toString()
        }

        return tripsRef.add(trip)
    }

    // rider requesting new ride
    @RequiresApi(Build.VERSION_CODES.O)
    override fun createTripPosting(userId: String, startingCoordinateId: String, endingCoordinateId: String, tripDate: LocalDate, isRecurring: Boolean, recurringDayOfTheWeek: DayOfTheWeek, recurringEndDate: LocalDate): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val trip = hashMapOf(
            "id" to id,
            "starting_coordinate" to startingCoordinateId,
            "ending_coordinate" to endingCoordinateId,
            "is_recurring" to isRecurring,
            "trip_date" to tripDate.toString(),
            "passengers" to listOf<String>(userId)
        )

        if (isRecurring) {
            trip["recurring_day"] = recurringDayOfTheWeek.day
            trip["recurring_end_date"] = recurringEndDate.toString()
        }

        return tripsRef.add(trip)
    }


    override fun createTripConfirmation(tripId: String, confirmationDate: LocalDate, riderId: String): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val confirmation = hashMapOf(
            "id" to id,
            "tripId" to tripId,
            "confirmation_date" to confirmationDate.toString(),
            "rider_id" to riderId
        )

        return tripsConfirmationRef.add(confirmation)
    }


    // get trips by driver id
    override fun fetchTripsByDriverId(driverId: String): Task<QuerySnapshot> {
        return tripsRef.whereEqualTo("driver_id", driverId).get()
    }

    // get trips by user id
    override fun fetchTripsByRiderId(riderId: String): Task<QuerySnapshot> {
        return tripsRef.whereArrayContains("passengers", riderId).get()
    }

    // get trip confirmation by rider id
    override fun fetchTripConfirmationByRiderId(riderId: String): Task<QuerySnapshot> {
        return tripsRef.whereEqualTo("rider_id", riderId).get()
    }


    private fun tripUpdate(tripId: String, tripUpdate: Map<String, Any>): Task<Void> {
        return tripsRef.whereEqualTo("id", tripId)
            .get()
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents
                    if (!documents.isNullOrEmpty()) {
                        documents[0].reference.update(tripUpdate)
                    } else {
                        throw Exception("Trip not found")
                    }
                } else {
                    throw task.exception ?: Exception("Failed to fetch trip")
                }
            }
    }
    // driver accepting posting
    override fun acceptTripPosting(driverId: String, tripId: String): Task<Void> {
        val tripUpdate = hashMapOf(
            "driver_id" to driverId
        )

        return tripUpdate(tripId, tripUpdate)
    }

    // update start/end coords
    override fun updateTripCoordinates(tripId: String, startingCoordinateId: String, endingCoordinateId: String): Task<Void> {
        val tripUpdate = hashMapOf(
            "starting_coordinate" to startingCoordinateId,
            "ending_coordinate" to endingCoordinateId,
        )

        return tripUpdate(tripId, tripUpdate)
    }

    // update num passengers
    override fun updatePassengerCount(tripId: String, newCount: Int): Task<Void> {
        if (newCount < 1) {
            throw Exception("Need to accept at least one passenger")
        }
        val tripUpdate = hashMapOf(
            "max_passengers" to newCount
        )

        return tripUpdate(tripId, tripUpdate)
    }

    // update date

    @RequiresApi(Build.VERSION_CODES.O)
    override fun updateTripDate(tripId: String, date: LocalDate): Task<Void> {
        if (date < LocalDate.now()) {
            throw Exception("Can only edit trip in the future")
        }
        val tripUpdate = hashMapOf(
            "trip_date" to date.toString()
        )

        return tripUpdate(tripId, tripUpdate)
    }

    // make trip recurring
    @RequiresApi(Build.VERSION_CODES.O)
    override fun makeTripRecurring(tripId: String, recurringDayOfTheWeek: DayOfTheWeek, recurringEndDate: LocalDate): Task<Void> {
        if (recurringEndDate < LocalDate.now()) {
            throw Exception("End date has to be in the future")
        }
        val tripUpdate = hashMapOf(
            "is_recurring" to true,
            "recurring_day" to recurringDayOfTheWeek.day,
            "recurring_end_date" to recurringEndDate.toString()
        )

        return tripUpdate(tripId, tripUpdate)
    }

    // disable recurring trip
    @RequiresApi(Build.VERSION_CODES.O)
    override fun makeTripNonRecurring(tripId: String): Task<Void> {

        val tripUpdate = hashMapOf(
            "is_recurring" to false,
            "recurring_day" to "",
            "recurring_end_date" to ""
        )

        return tripUpdate(tripId, tripUpdate)
    }

    // ------------------ DELETE (TODO) ------------------

    // delete trip confirmation

    // delete trip posting - driver side

    // delete trip posting - rider side

}

