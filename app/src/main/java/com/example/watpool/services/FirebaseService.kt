package com.example.watpool.services
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
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
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.database.database
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import java.time.LocalDate


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
    fun fetchUsersByUsername(username: String) : Task<QuerySnapshot> {
        return userService.fetchUsersByUsername(username)
    }
    fun createUser(username: String, name: String, phone: String) : Task<DocumentReference> {
        return userService.createUser(username, name, phone)
    }
    fun createDriver(username: String, licenseNumber: String, carModel: String, carColor: String ) : Task<Void> {
        return userService.createDriver(username, licenseNumber, carModel, carColor)
    }

//    fun cloneTrip(tripId: String) {
//        tripsService.cloneTrip(tripId)
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createTrip(driverId: String, startingCoordinateId: String, endingCoordinateId: String, tripDate: LocalDate, maxPassengers: String, isRecurring: Boolean = false, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY, recurringEndDate: LocalDate = LocalDate.now()): Task<DocumentReference> {
        return tripsService.createTrip(driverId, startingCoordinateId, endingCoordinateId, tripDate, maxPassengers, isRecurring, recurringDayOfTheWeek, recurringEndDate)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun createTripPosting(userId: String, startingCoordinateId: String, endingCoordinateId: String, tripDate: LocalDate, isRecurring: Boolean = false, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek = FirebaseTripsService.DayOfTheWeek.SUNDAY, recurringEndDate: LocalDate = LocalDate.now()): Task<DocumentReference> {
        return tripsService.createTripPosting(userId, startingCoordinateId, endingCoordinateId, tripDate, isRecurring, recurringDayOfTheWeek, recurringEndDate)
    }
    fun createTripConfirmation(tripId: String, confirmationDate: LocalDate, riderId: String): Task<DocumentReference> {
        return tripsService.createTripConfirmation(tripId, confirmationDate, riderId)
    }

    private fun tripsCoordinateConnector(task: Task<QuerySnapshot>): Task<List<DocumentSnapshot>> {
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
                                coordinateService.fetchCoordinatesById(endCoordinateId)
                            ).continueWith { coordTasks ->
                                val startLocation = (coordTasks.result[0].result as? DocumentSnapshot)?.getString("location") ?: ""
                                val endLocation = (coordTasks.result[1].result as? DocumentSnapshot)?.getString("location") ?: ""

                                document.reference.update(
                                    mapOf(
                                        "to" to startLocation,
                                        "from" to endLocation
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
    fun makeTripRecurring(tripId: String, recurringDayOfTheWeek: FirebaseTripsService.DayOfTheWeek, recurringEndDate: LocalDate): Task<Void> {
        return tripsService.makeTripRecurring(tripId, recurringDayOfTheWeek, recurringEndDate)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun makeTripNonRecurring(tripId: String): Task<Void> {
        return tripsService.makeTripNonRecurring(tripId)
    }

}