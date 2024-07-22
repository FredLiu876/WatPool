package com.example.watpool.services.firebase_services


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.watpool.services.interfaces.TripsService
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.util.UUID
import com.google.firebase.firestore.FieldValue
import java.time.LocalTime

class FirebaseTripsService: TripsService {
    private val database = Firebase.firestore
    private val tripsRef: CollectionReference = database.collection("trips")
    private val tripsConfirmationRef: CollectionReference = database.collection("trips_confirmation")
    private val coordinatesRef: CollectionReference = database.collection("coordinates")

    enum class DayOfTheWeek(val day: String) {
        SUNDAY ("SUNDAY"), MONDAY ("MONDAY"), TUESDAY ("TUESDAY"),
        WEDNESDAY ("WEDNESDAY"), THURSDAY ("THURSDAY"), FRIDAY ("FRIDAY"), SATURDAY ("SATURDAY")
    }

    // driver posting a new trip
    @RequiresApi(Build.VERSION_CODES.O)
    override fun createTrip(
        driverId: String,
        startingCoordinateId: String,
        endingCoordinateId: String,
        startGeohash: String,
        endGeohash: String,
        tripDate: LocalDate,
        maxPassengers: String,
        isRecurring: Boolean,
        recurringDayOfTheWeek: DayOfTheWeek,
        recurringEndDate: LocalDate,
        tripTime: LocalTime
    ): Task<DocumentReference> {

        val id: String = UUID.randomUUID().toString()
        val trip = hashMapOf(
            "id" to id,
            "starting_coordinate" to startingCoordinateId,
            "ending_coordinate" to endingCoordinateId,
            "max_passengers" to maxPassengers,
            "is_recurring" to isRecurring,
            "trip_date" to tripDate.toString(),
            "trip_time" to tripTime.toString(),
            "driver_id" to driverId,
            "start_geohash" to startGeohash,
            "end_geohash" to endGeohash
        )

        if (isRecurring) {
            trip["recurring_day"] = recurringDayOfTheWeek.day
            trip["recurring_end_date"] = recurringEndDate.toString()
        }

        return tripsRef.add(trip)
    }

    // rider requesting new ride
    @RequiresApi(Build.VERSION_CODES.O)
    override fun createTripPosting(userId: String, startingCoordinateId: String, endingCoordinateId: String, startGeohash: String, endGeohash: String, tripDate: LocalDate, isRecurring: Boolean, recurringDayOfTheWeek: DayOfTheWeek, recurringEndDate: LocalDate, tripTime: LocalTime): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val trip = hashMapOf(
            "id" to id,
            "starting_coordinate" to startingCoordinateId,
            "ending_coordinate" to endingCoordinateId,
            "is_recurring" to isRecurring,
            "trip_date" to tripDate.toString(),
            "trip_time" to tripTime.toString(),
            "passengers" to listOf<String>(userId),
            "start_geohash" to startGeohash,
            "end_geohash" to endGeohash
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
    override fun fetchTripsByTripIds(tripIds: List<String>): Task<QuerySnapshot> {
        return tripsRef.whereIn("id", tripIds).get()
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
        return tripsConfirmationRef.whereEqualTo("rider_id", riderId).get()
    }

    override fun fetchTripConfirmationByTripIdAndRiderId(tripId: String, riderId: String): Task<QuerySnapshot> {
        return tripsConfirmationRef.whereEqualTo("tripId", tripId).whereEqualTo("rider_id", riderId).get()
    }

    override fun deleteTripConfirmation(documentId: String) {
        tripsConfirmationRef.document(documentId).delete()
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

    override fun addPassenger(tripId: String, newPassengerId: String): Task<Void> {
        val tripUpdate = hashMapOf(
            "passengers" to FieldValue.arrayUnion(newPassengerId)
        )

        return tripUpdate(tripId, tripUpdate)
    }

    override fun removePassenger(tripId: String, passengerId: String): Task<Void> {
        val tripUpdate = hashMapOf(
            "passengers" to FieldValue.arrayRemove(passengerId)
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

    override fun updateTripTime(tripId: String, time: LocalTime): Task<Void> {
        val tripUpdate = hashMapOf(
            "trip_time" to time.toString()
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

    override fun fetchTripsByLocation(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double,
        startFilter: Boolean
    ): Task<MutableList<DocumentSnapshot>> {
        // Find the bounding box for quick filtering
        val center = GeoLocation(latitude, longitude)
        val radiusInM = radiusInKm * 1000

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()

        var filterName: String = "start_geohash"
        if (!startFilter) {
            filterName = "end_geohash"
        }
        for (b in bounds) {
            val q = tripsRef
                .orderBy(filterName)
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        return Tasks.whenAllComplete(tasks)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Unknown error")
                }

                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                val coordTasks: MutableList<Task<DocumentSnapshot>> = ArrayList()

                for (queryTask in tasks) {
                    val snap = queryTask.result
                    var coordFilterName = "starting_coordinate"
                    if (!startFilter) {
                        coordFilterName = "ending_coordinate"
                    }
                    for (doc in snap!!.documents) {
                        val coordTask = coordinatesRef.document(doc.getString(coordFilterName)!!).get()
                            .continueWithTask { coordSnapshotTask ->
                                if (coordSnapshotTask.isSuccessful) {
                                    val coordSnapshot = coordSnapshotTask.result
                                    val tripLat = coordSnapshot?.getDouble("latitude")
                                    val tripLng = coordSnapshot?.getDouble("longitude")

                                    if (tripLat != null && tripLng != null) {
                                        val docLocation = GeoLocation(tripLat, tripLng)
                                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                                        if (distanceInM <= radiusInM) {
                                            synchronized(matchingDocs) {
                                                matchingDocs.add(doc)
                                            }
                                        }
                                    }
                                } else {
                                    Log.e("Trip Fetch Error", "Error getting coordinates: ", coordSnapshotTask.exception)
                                }
                                coordSnapshotTask
                            }
                        coordTasks.add(coordTask)
                    }
                }

                Tasks.whenAllComplete(coordTasks)
                    .continueWith { _ ->
                        matchingDocs
                    }
            }
    }

    override fun fetchTripsByStartEnd(
        startLatitude: Double,
        startLongitude: Double,
        startRadiusInKm: Double,
        endLatitude: Double,
        endLongitude: Double,
        endRadiusInKm: Double
    ): Task<MutableList<DocumentSnapshot>> {
        val startCenter = GeoLocation(startLatitude, startLongitude)
        val endCenter = GeoLocation(endLatitude, endLongitude)
        val startRadiusInM = startRadiusInKm * 1000
        val endRadiusInM = endRadiusInKm * 1000

        val startBounds = GeoFireUtils.getGeoHashQueryBounds(startCenter, startRadiusInM)
        val endBounds = GeoFireUtils.getGeoHashQueryBounds(endCenter, endRadiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()

        val allBounds = (startBounds + endBounds).distinct()

        for (b in allBounds) {
            val q = tripsRef
                .orderBy("start_geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        return Tasks.whenAllComplete(tasks)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Unknown error")
                }

                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                val coordTasks: MutableList<Task<Void>> = ArrayList()
                val uniqueDocIds: MutableSet<String> = mutableSetOf()

                for (queryTask in tasks) {
                    val snap = queryTask.result
                    for (doc in snap!!.documents) {
                        if (uniqueDocIds.contains(doc.id)) {
                            continue
                        }

                        val startCoordTask = coordinatesRef.document(doc.getString("starting_coordinate")!!).get()
                        val endCoordTask = coordinatesRef.document(doc.getString("ending_coordinate")!!).get()

                        coordTasks.add(
                            Tasks.whenAllComplete(startCoordTask, endCoordTask)
                                .continueWithTask { coordSnapshotTasks ->
                                    if (startCoordTask.isSuccessful && endCoordTask.isSuccessful) {
                                        val startCoordSnapshot = startCoordTask.result
                                        val endCoordSnapshot = endCoordTask.result

                                        val startLat = startCoordSnapshot?.getDouble("latitude")
                                        val startLng = startCoordSnapshot?.getDouble("longitude")
                                        val endLat = endCoordSnapshot?.getDouble("latitude")
                                        val endLng = endCoordSnapshot?.getDouble("longitude")

                                        if (startLat != null && startLng != null && endLat != null && endLng != null) {
                                            val startLocation = GeoLocation(startLat, startLng)
                                            val endLocation = GeoLocation(endLat, endLng)
                                            val startDistanceInM = GeoFireUtils.getDistanceBetween(startLocation, startCenter)
                                            val endDistanceInM = GeoFireUtils.getDistanceBetween(endLocation, endCenter)

                                            if (startDistanceInM <= startRadiusInM && endDistanceInM <= endRadiusInM) {
                                                synchronized(matchingDocs) {
                                                    if (!uniqueDocIds.contains(doc.id)) {
                                                        matchingDocs.add(doc)
                                                        uniqueDocIds.add(doc.id)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Log.e("Trip Fetch Error", "Error getting coordinates: ", coordSnapshotTasks.exception)
                                    }
                                    Tasks.forResult<Void>(null)
                                }
                        )
                    }
                }

                Tasks.whenAllComplete(coordTasks)
                    .continueWith { _ ->
                        matchingDocs
                    }
            }
    }



}

