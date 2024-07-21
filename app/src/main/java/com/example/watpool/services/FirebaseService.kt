package com.example.watpool.services
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.watpool.services.firebase_services.FirebaseAuthService
import com.example.watpool.services.firebase_services.FirebaseCoordinateService
import com.example.watpool.services.firebase_services.FirebaseDriverService
import com.example.watpool.services.firebase_services.FirebaseTripsService
import com.example.watpool.services.firebase_services.FirebaseUserService
import com.example.watpool.services.interfaces.AuthService
import com.example.watpool.services.interfaces.CoordinateService
import com.example.watpool.services.interfaces.DriverService
import com.example.watpool.services.interfaces.TripsService
import com.example.watpool.services.interfaces.UserService
import com.example.watpool.services.models.Coordinate
import com.example.watpool.services.models.TripConfirmationDetails
import com.example.watpool.ui.profile.UserDetails
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.database.database
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale


// TODO: Add helper functions here to interact with the DB
class FirebaseService : Service() {
    private lateinit var authService: AuthService
    private lateinit var driverService: DriverService
    private lateinit var coordinateService: CoordinateService
    private lateinit var userService: UserService
    private lateinit var tripsService: TripsService


    inner class FirebaseBinder : Binder() {
        fun getService() = this@FirebaseService
    }

    private val binder: IBinder = FirebaseBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        authService = FirebaseAuthService()
        driverService = FirebaseDriverService()
        coordinateService = FirebaseCoordinateService()
        userService = FirebaseUserService()
        tripsService = FirebaseTripsService()
    }

    fun signIn(email: String, password: String): Task<AuthResult> {
        return authService.signIn(email, password)
    }

    fun signUp(email: String, password: String): Task<AuthResult> {
        return authService.signUp(email, password)
    }

    fun currentUserFull(): Task<QuerySnapshot> {
        val user = authService.getCurrentUser()

        if (user == null) {
            throw Exception("This is unexpected, user should be logged in")
        }

        return userService.fetchUsersByEmail(user.email ?: "")
    }

    suspend fun currentUser(): String {
        return currentUserFull().await().documents[0].getString("id")
            ?: throw Exception("This is unexpected, user should have id value")
    }

    fun fetchUserByDocumentId(id: String): Task<DocumentSnapshot> {
        return userService.fetchUserByDocumentId(id)
    }


    fun fetchCoordinatesByDocumentId(id: String): Task<DocumentSnapshot> {
        return coordinateService.fetchCoordinatesByDocumentId(id)
    }


    fun fetchCoordinatesByDriverId(driverId: String): Task<QuerySnapshot> {
        return coordinateService.fetchCoordinatesByDriverId(driverId)
    }

    fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<MutableList<DocumentSnapshot>> {
        return coordinateService.fetchCoordinatesByLocation(latitude, longitude, radiusInKm)
    }

    fun addCoordinate(driverId: String, latitude: Double, longitude: Double, location: String = ""): Task<DocumentReference> {
        return coordinateService.addCoordinate(driverId, latitude, longitude, location)
    }

    fun fetchUsersById(id: String): Task<QuerySnapshot> {
        return userService.fetchUsersById(id)
    }
    fun fetchUsersByEmail(email: String) : Task<QuerySnapshot> {
        return userService.fetchUsersByEmail(email)
    }
    fun createUser(email: String, name: String) : Task<DocumentReference> {
        return userService.createUser(email, name)
    }
    fun createDriver(username: String, licenseNumber: String, carModel: String, carColor: String ) : Task<Void> {
        return userService.createDriver(username, licenseNumber, carModel, carColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createTrip(
        driverId: String,
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        startLocation: String,
        endLocation: String,
        tripDate: LocalDate,
        maxPassengers: String,
        isRecurring: Boolean = false,
        recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY,
        recurringEndDate: LocalDate = LocalDate.now(),
        tripTime: LocalTime
    ): Task<Task<DocumentReference>> {
        return Tasks.whenAllComplete(
            coordinateService.addCoordinate(driverId, startLatitude, startLongitude, startLocation),
            coordinateService.addCoordinate(driverId, endLatitude, endLongitude, endLocation)
        ).continueWith { tasks ->
            if (tasks.isSuccessful) {
                val startingCoordinateId = (tasks.result[0].result as? DocumentReference)?.id ?: ""
                val endingCoordinateId = (tasks.result[1].result as? DocumentReference)?.id ?: ""
                val startGeohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(startLatitude, startLongitude))
                val endGeohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(endLatitude, endLongitude))
                tripsService.createTrip(
                    driverId,
                    startingCoordinateId,
                    endingCoordinateId,
                    startGeohash,
                    endGeohash,
                    tripDate,
                    maxPassengers,
                    isRecurring,
                    recurringDayOfTheWeek,
                    recurringEndDate,
                    tripTime
                )
            } else {
                Tasks.forException(tasks.exception ?: Exception("Unknown error"))
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun createTripPosting(userId: String, startLatitude: Double, endLatitude: Double, startLongitude: Double, endLongitude: Double, startLocation: String, endLocation: String, tripDate: LocalDate, isRecurring: Boolean = false, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY, recurringEndDate: LocalDate = LocalDate.now(), tripTime: LocalTime = LocalTime.now()): Task<Task<DocumentReference>> {
        return Tasks.whenAllComplete(
            coordinateService.addCoordinate(userId, startLatitude, startLongitude, startLocation),
            coordinateService.addCoordinate(userId, endLatitude, endLongitude, endLocation)
        ).continueWith { tasks ->
            if (tasks.isSuccessful) {
                val startingCoordinateId =
                    (tasks.result[0].result as? DocumentSnapshot)?.getString("id") ?: ""
                val endingCoordinateId =
                    (tasks.result[1].result as? DocumentSnapshot)?.getString("id") ?: ""
                val startGeohash =
                    GeoFireUtils.getGeoHashForLocation(GeoLocation(startLatitude, startLongitude))
                val endGeohash =
                    GeoFireUtils.getGeoHashForLocation(GeoLocation(endLatitude, endLongitude))
                tripsService.createTripPosting(
                    userId,
                    startingCoordinateId,
                    endingCoordinateId,
                    startGeohash,
                    endGeohash,
                    tripDate,
                    isRecurring,
                    recurringDayOfTheWeek,
                    recurringEndDate,
                    tripTime
                )
            } else {
                Tasks.forException(tasks.exception ?: Exception("Unknown error"))
            }
        }
    }
    fun createTripConfirmation(tripId: String, confirmationDate: LocalDate, riderId: String): Task<DocumentReference> {
        return tripsService.createTripConfirmation(tripId, confirmationDate, riderId)
    }

    fun tripsCoordinateConnector(task: Task<QuerySnapshot>): Task<List<DocumentSnapshot>> {
        return task
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    if (!querySnapshot.isEmpty) {
                        val updateTasks = querySnapshot.documents.map { document ->
                            val startCoordinateId = document.getString("starting_coordinate") ?: ""
                            val endCoordinateId = document.getString("ending_coordinate") ?: ""

                            Tasks.whenAllComplete(
                                coordinateService.fetchCoordinatesById(startCoordinateId),
                                coordinateService.fetchCoordinatesById(endCoordinateId),
                                coordinateService.fetchCoordinatesByDocumentId(startCoordinateId),
                                coordinateService.fetchCoordinatesByDocumentId(endCoordinateId)
                            ).continueWith { coordTasks ->
                                var startDoc = if ((coordTasks.result[0].result as? QuerySnapshot)?.documents?.firstOrNull() != null) {
                                    (coordTasks.result[0].result as? QuerySnapshot)?.documents?.firstOrNull()
                                } else {
                                    (coordTasks.result[2].result as? QuerySnapshot)?.documents?.firstOrNull()
                                }

                                var endDoc = if ((coordTasks.result[0].result as? QuerySnapshot)?.documents?.firstOrNull() != null) {
                                    (coordTasks.result[1].result as? QuerySnapshot)?.documents?.firstOrNull()
                                } else {
                                    (coordTasks.result[3].result as? QuerySnapshot)?.documents?.firstOrNull()
                                }

                                val startLocation = startDoc?.getString("location") ?: ""
                                val endLocation = endDoc?.getString("location") ?: ""

                                val startLatitude = startDoc?.getDouble("latitude") ?: 0.0
                                val startLongitude = startDoc?.getDouble("longitude") ?: 0.0

                                val endLatitude = endDoc?.getDouble("latitude") ?: 0.0
                                val endLongitude = endDoc?.getDouble("longitude") ?: 0.0
                                document.reference.update(
                                    mapOf(
                                        "to" to startLocation,
                                        "from" to endLocation,
                                        "startLatLng" to LatLng(startLatitude, startLongitude),
                                        "endLatLng" to LatLng(endLatitude, endLongitude)
                                    )
                                )
                                document
                            }
                        }
                        Tasks.whenAllComplete(updateTasks)
                            .continueWith { it -> it.result.map { it.result as DocumentSnapshot } }
                    } else {
                        Tasks.forResult(emptyList<DocumentSnapshot>())
                    }
                } else {
                    Tasks.forException(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    fun fetchTripsByTripsId(tripIds: List<String>): Task<List<DocumentSnapshot>> {
        return tripsCoordinateConnector(tripsService.fetchTripsByTripIds(tripIds))
    }

    fun fetchTripsByDriverId(driverId: String): Task<List<DocumentSnapshot>> {
        return tripsCoordinateConnector(tripsService.fetchTripsByDriverId(driverId))
    }

    fun fetchTripsByRiderId(riderId: String): Task<List<DocumentSnapshot>> {
        return tripsCoordinateConnector(tripsService.fetchTripsByRiderId(riderId))
    }

    fun fetchTripConfirmationByRiderId(riderId: String): Task<QuerySnapshot> {
        return tripsService.fetchTripConfirmationByRiderId(riderId)
    }

    fun acceptTripPosting(driverId: String, tripId: String): Task<Void> {
        return tripsService.acceptTripPosting(driverId, tripId)
    }

    fun updateTripCoordinates(tripId: String, startingCoordinateId: String, endingCoordinateId: String): Task<Void> {
        return tripsService.updateTripCoordinates(tripId, startingCoordinateId, endingCoordinateId)
    }
    // update num passengers
    fun updatePassengerCount(tripId: String, newCount: Int): Task<Void> {
        return tripsService.updatePassengerCount(tripId, newCount)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTripDate(tripId: String, date: LocalDate): Task<Void> {
        return tripsService.updateTripDate(tripId, date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTripTime(tripId: String, time: LocalTime): Task<Void> {
        return tripsService.updateTripTime(tripId, time)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeTripRecurring(tripId: String, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek, recurringEndDate: LocalDate): Task<Void> {
        return tripsService.makeTripRecurring(tripId, recurringDayOfTheWeek, recurringEndDate)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeTripNonRecurring(tripId: String): Task<Void> {
        return tripsService.makeTripNonRecurring(tripId)
    }

    suspend fun fetchAllConfirmedTripsByRiderId(riderId: String): List<TripConfirmationDetails> {
        val tripConfirmations = tripsService.fetchTripConfirmationByRiderId(riderId).await()
        val tripIds = tripConfirmations.documents.mapNotNull { it.getString("tripId") }
        val tripDetails = tripsCoordinateConnector(tripsService.fetchTripsByTripIds(tripIds)).await()


        val fetchedTrips = tripDetails.mapNotNull { document ->
            val tripDateFormatted = document.getTimestamp("trip_date")?.toDate()
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            Log.d("TripDocument", "ID: ${document.getString("id")}, To: ${document.getString("to")}, From: ${document.getString("from")}, Trip Date: $tripDateFormatted, Driver ID: ${document.getString("driver_id")}")
            val documentData = document.data
            Log.d("TripDocument", documentData.toString())

            tripDateFormatted?.let {
                TripConfirmationDetails(
                    id = document.getString("id") ?: "",
                    to = document.getString("to") ?: "",
                    from = document.getString("from") ?: "",
                    tripDate = format.format(tripDateFormatted),
                    driverId = document.getString("driver_id") ?: "",
                    driver = ""
                )
            }
        }
        val fullTripDetails = fetchedTrips.map { trip ->
            val driverName = userService.fetchUsersById(trip.driverId).await().documents.mapNotNull { it.getString("name") } [0]
            trip.copy(driver = driverName ?: "Unknown")
        }
        return fullTripDetails
    }
      
    // Specify a location and a range to get trips starting / ending within that range
    // fetches by start filter by default, set to false to fetch by end coordinates
    fun fetchTripsByLocation(latitude: Double, longitude: Double, radiusInKm: Double, fetchByStart: Boolean = true): Task<MutableList<DocumentSnapshot>> {
        return tripsService.fetchTripsByLocation(latitude, longitude, radiusInKm, fetchByStart)
    }


    fun addPassenger(tripId: String, newPassengerId: String): Task<Void> {
        return tripsService.addPassenger(tripId, newPassengerId)
    }

    fun removePassenger(tripId: String, passengerId: String): Task<Void> {
        return tripsService.addPassenger(tripId, passengerId)
    }

    fun fetchTripsByStartAndEnd(latitudeStart: Double, longitudeStart: Double, radiusInKmStart: Double, latitudeEnd: Double, longitudeEnd: Double, radiusInKmEnd: Double): Task<MutableList<DocumentSnapshot>>{
        return tripsService.fetchTripsByStartEnd(latitudeStart, longitudeStart, radiusInKmStart, latitudeEnd, longitudeEnd, radiusInKmEnd)
    }

    fun tripsCoordinateConnectorLocation(task: Task<MutableList<DocumentSnapshot>>): Task<MutableList<DocumentSnapshot>> {
        return task
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    val documents = task.result
                    if (documents.isNotEmpty()) {
                        val updateTasks = documents.map { document ->
                            val startCoordinateId = document.getString("starting_coordinate") ?: ""
                            val endCoordinateId = document.getString("ending_coordinate") ?: ""

                            Tasks.whenAllComplete(
                                coordinateService.fetchCoordinatesById(startCoordinateId),
                                coordinateService.fetchCoordinatesById(endCoordinateId)
                            ).continueWith { coordTasks ->
                                val startLocation = (coordTasks.result[0].result as? QuerySnapshot)?.documents?.firstOrNull()?.getString("location") ?: ""
                                val endLocation = (coordTasks.result[1].result as? QuerySnapshot)?.documents?.firstOrNull()?.getString("location") ?: ""
                                val startLatitude = (coordTasks.result[0].result as? QuerySnapshot)?.documents?.firstOrNull()?.getDouble("latitude") ?: 0.0
                                val startLongitude = (coordTasks.result[0].result as? QuerySnapshot)?.documents?.firstOrNull()?.getDouble("longitude") ?: 0.0
                                val endLatitude = (coordTasks.result[1].result as? QuerySnapshot)?.documents?.firstOrNull()?.getDouble("latitude") ?: 0.0
                                val endLongitude = (coordTasks.result[1].result as? QuerySnapshot)?.documents?.firstOrNull()?.getDouble("longitude") ?: 0.0
                                document.reference.update(
                                    mapOf(
                                        "to" to startLocation,
                                        "from" to endLocation,
                                        "startLatLng" to LatLng(startLatitude, startLongitude),
                                        "endLatLng" to LatLng(endLatitude, endLongitude)
                                    )
                                )
                                document
                            }
                        }
                        Tasks.whenAllComplete(updateTasks)
                            .continueWith { it -> it.result.map { it.result as DocumentSnapshot } as MutableList<DocumentSnapshot> }
                    } else {
                        Tasks.forResult(mutableListOf<DocumentSnapshot>())
                    }
                } else {
                    Tasks.forException(task.exception ?: Exception("Unknown error"))
                }
            }
    }



}